/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.ForceDisconnectException;
import com.newrelic.agent.ForceRestartException;
import com.newrelic.agent.LicenseException;
import com.newrelic.agent.MaxPayloadException;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.DataSenderConfig;
import com.newrelic.agent.config.LaspPolicies;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.agentcontrol.AgentHealth;
import com.newrelic.agent.agentcontrol.HealthDataChangeListener;
import com.newrelic.agent.agentcontrol.HealthDataProducer;
import com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils;
import com.newrelic.agent.trace.TransactionTrace;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.UnexpectedException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static com.newrelic.agent.util.LicenseKeyUtil.obfuscateLicenseKey;

/**
 * A class for sending and receiving New Relic data.
 *
 * This class is thread-safe.
 */
public class DataSenderImpl implements DataSender, HealthDataProducer {

    private static final String MODULE_TYPE = "Jars";
    private static final int PROTOCOL_VERSION = 17;
    private static final String PROTOCOL = "https";
    private static final String BEFORE_LICENSE_KEY_URI_PATTERN = "/agent_listener/invoke_raw_method?method={0}";
    private static final String AFTER_LICENSE_KEY_URI_PATTERN = "&marshal_format=json&protocol_version=";
    private static final String LICENSE_KEY_URI_PATTERN = "&license_key={0}";
    private static final String RUN_ID_PATTERN = "&run_id={1}";

    public static final String DEFLATE_ENCODING = "deflate";
    public static final String GZIP_ENCODING = "gzip";
    private static final String IDENTITY_ENCODING = "identity";
    private static final String EXCEPTION_MAP_RETURN_VALUE_KEY = "return_value";
    private static final Object NO_AGENT_RUN_ID = null;
    private static final String NULL_RESPONSE = "null";
    private static final int COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
    private static final String REDIRECT_HOST = "redirect_host";
    private static final String SECURITY_POLICIES = "security_policies";
    private static final String MAX_PAYLOAD_SIZE_IN_BYTES = "max_payload_size_in_bytes";
    // to query the environment variables
    private static final String METADATA_PREFIX = "NEW_RELIC_METADATA_";
    // the block of env vars we send up to rpm
    private static final String ENV_METADATA = "metadata";
    private static final int DEFAULT_MAX_PAYLOAD_SIZE_IN_BYTES = 1_000_000;

    // Destinations for agent data
    private static final String COLLECTOR = "Collector";

    // As of P17 these are the only agent endpoints that actually contain data in the response payload for a successful request
    private static final Set<String> METHODS_WITH_RESPONSE_BODY = ImmutableSet.of(
            CollectorMethods.PRECONNECT,
            CollectorMethods.CONNECT,
            CollectorMethods.GET_AGENT_COMMANDS,
            CollectorMethods.PROFILE_DATA);

    private final HttpClientWrapper httpClientWrapper;

    private final String originalHost;
    private volatile String redirectHost;
    private final int port;

    private volatile boolean auditMode;
    private Set<String> auditModeEndpoints;
    private final IAgentLogger logger;
    private final ConfigService configService;
    private volatile Object agentRunId = NO_AGENT_RUN_ID;
    private final String agentRunIdUriPattern;
    private final String noAgentRunIdUriPattern;
    private final DataSenderListener dataSenderListener;
    private final String compressedEncoding;
    private final boolean putForDataSend;
    private Map<String, Boolean> policiesJson;
    private volatile int maxPayloadSizeInBytes = DEFAULT_MAX_PAYLOAD_SIZE_IN_BYTES;
    private volatile Map<String, String> requestMetadata;
    private volatile Map<String, String> metadata;
    private final List<HealthDataChangeListener> healthDataChangeListeners = new CopyOnWriteArrayList<>();
    private final boolean isAgentControlEnabled;

    public DataSenderImpl(
            DataSenderConfig config,
            HttpClientWrapper httpClientWrapper,
            DataSenderListener dataSenderListener,
            IAgentLogger logger,
            ConfigService configService) {
        auditMode = config.isAuditMode();
        auditModeEndpoints = config.getAuditModeConfig().getEndpoints();
        this.logger = logger;
        this.configService = configService;
        logger.info(MessageFormat.format("Setting audit_mode to {0}", auditMode));
        originalHost = config.getHost();
        redirectHost = config.getHost();
        port = config.getPort();

        String licenseKeyUri = MessageFormat.format(LICENSE_KEY_URI_PATTERN, config.getLicenseKey());
        noAgentRunIdUriPattern = BEFORE_LICENSE_KEY_URI_PATTERN + licenseKeyUri + AFTER_LICENSE_KEY_URI_PATTERN + PROTOCOL_VERSION;
        agentRunIdUriPattern = noAgentRunIdUriPattern + RUN_ID_PATTERN;
        this.dataSenderListener = dataSenderListener;
        this.compressedEncoding = config.getCompressedContentEncoding();
        this.putForDataSend = config.isPutForDataSend();

        this.metadata = new HashMap<>();
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (entry.getKey().startsWith(METADATA_PREFIX)) {
                this.metadata.put(entry.getKey(), entry.getValue());
            }
        }

        this.httpClientWrapper = httpClientWrapper;
        this.isAgentControlEnabled = configService.getDefaultAgentConfig().getAgentControlIntegrationConfig().isEnabled();
    }

    private void checkAuditMode() {
        boolean auditMode2 = configService.getLocalAgentConfig().isAuditMode();
        if (auditMode != auditMode2) {
            auditMode = auditMode2;
            logger.info(MessageFormat.format("Setting audit_mode to {0}", auditMode));
        }

        Set<String> auditModeEndpoints2 = configService
                .getLocalAgentConfig()
                .getAuditModeConfig()
                .getEndpoints();
        if (auditModeEndpoints != auditModeEndpoints2) {
            auditModeEndpoints = auditModeEndpoints2;
            logger.info(MessageFormat.format("Setting audit_mode.endpoints to {0}", auditModeEndpoints));
        }
    }

    @VisibleForTesting
    void setAgentRunId(Object runId) {
        agentRunId = runId;
        if (runId != NO_AGENT_RUN_ID) {
            logger.info("Agent run id: " + runId);
        }
    }

    @VisibleForTesting
    Object getAgentRunId() {
        return agentRunId;
    }

    @Override
    public Map<String, Object> connect(Map<String, Object> startupOptions) throws Exception {
        String redirectHost = parsePreconnectAndReturnHost();
        if (redirectHost != null) {
            this.redirectHost = redirectHost;
            logger.info(MessageFormat.format("Collector redirection to {0}:{1}", this.redirectHost, Integer.toString(port)));
        } else if (configService.getDefaultAgentConfig().laspEnabled()) {
            throw new ForceDisconnectException("The agent did not receive one or more security policies that it expected and will shut down."
                    + " Please contact support.");
        }
        return doConnect(startupOptions);
    }

    private String parsePreconnectAndReturnHost() throws Exception {
        AgentConfig agentConfig = configService.getDefaultAgentConfig();

        InitialSizedJsonArray params = new InitialSizedJsonArray(1);
        JSONObject token = new JSONObject();

        if (agentConfig.laspEnabled()) {
            token.put("security_policies_token", agentConfig.securityPoliciesToken());
        }
        token.put("high_security", agentConfig.isHighSecurity());
        params.add(token);
        Object response = invokeNoRunId(originalHost, CollectorMethods.PRECONNECT, compressedEncoding, params);

        if (response != null) {
            Map<?, ?> returnValue = (Map<?, ?>) response;
            String host = returnValue.get(REDIRECT_HOST).toString();

            JSONObject policies = (JSONObject) returnValue.get(SECURITY_POLICIES);
            this.policiesJson = LaspPolicies.validatePolicies(policies);

            return host;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doConnect(Map<String, Object> startupOptions) throws Exception {
        InitialSizedJsonArray params = new InitialSizedJsonArray(1);
        if (policiesJson != null && !policiesJson.isEmpty()) {
            startupOptions.put("security_policies", LaspPolicies.convertToConnectPayload(policiesJson));
        }

        startupOptions.put(ENV_METADATA, metadata);

        params.add(startupOptions);
        Object response = invokeNoRunId(redirectHost, CollectorMethods.CONNECT, compressedEncoding, params);
        if (!(response instanceof Map)) {
            throw new UnexpectedException(MessageFormat.format("Expected a map of connection data, got {0}", response));
        }
        Map<String, Object> data = (Map<String, Object>) response;
        if (data.containsKey(MAX_PAYLOAD_SIZE_IN_BYTES)) {
            Object maxPayloadSize = data.get(MAX_PAYLOAD_SIZE_IN_BYTES);
            if (maxPayloadSize instanceof Number) {
                maxPayloadSizeInBytes = ((Number) maxPayloadSize).intValue();
                logger.log(Level.INFO, "Max payload size is {0} bytes", maxPayloadSizeInBytes);
            }
        }

        if (data.containsKey(ConnectionResponse.REQUEST_HEADERS)) {
            final Object requestMetadata = data.get(ConnectionResponse.REQUEST_HEADERS);
            if (requestMetadata instanceof Map) {
                this.requestMetadata = (Map<String, String>) requestMetadata;
            } else {
                logger.log(Level.WARNING, "Expected a map but got {0}. Not setting requestMetadata", requestMetadata);
            }
        } else {
            logger.log(Level.WARNING, "Did not receive requestMetadata on connect");
        }

        if (data.containsKey(ConnectionResponse.AGENT_RUN_ID_KEY)) {
            Object runId = data.get(ConnectionResponse.AGENT_RUN_ID_KEY);
            setAgentRunId(runId);
        } else {
            throw new UnexpectedException(MessageFormat.format("Missing {0} connection parameter", ConnectionResponse.AGENT_RUN_ID_KEY));
        }
        configService.setLaspPolicies(policiesJson);

        return data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<List<?>> getAgentCommands() throws Exception {
        checkAuditMode();
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID) {
            return Collections.emptyList();
        }
        InitialSizedJsonArray params = new InitialSizedJsonArray(1);
        params.add(runId);

        Object response = invokeRunId(CollectorMethods.GET_AGENT_COMMANDS, compressedEncoding, runId, params);
        if (response == null || NULL_RESPONSE.equals(response)) {
            return Collections.emptyList();
        }
        try {
            return (List<List<?>>) response;
        } catch (ClassCastException e) {
            logger.warning(MessageFormat.format("Invalid response from New Relic when getting agent commands: {0}", e));
            throw e;
        }
    }

    @Override
    public void sendCommandResults(Map<Long, Object> commandResults) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || commandResults.isEmpty()) {
            return;
        }

        InitialSizedJsonArray params = new InitialSizedJsonArray(2);
        params.add(runId);
        params.add(commandResults);

        invokeRunId(CollectorMethods.AGENT_COMMAND_RESULTS, compressedEncoding, runId, params);
    }

    @Override
    public void sendErrorData(List<TracedError> errors) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || errors.isEmpty()) {
            return;
        }

        InitialSizedJsonArray params = new InitialSizedJsonArray(2);
        params.add(runId);
        params.add(errors);

        invokeRunId(CollectorMethods.ERROR_DATA, compressedEncoding, runId, params);
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> errorEvents) throws Exception {
        sendAnalyticEventsForReservoir(CollectorMethods.ERROR_EVENT_DATA, compressedEncoding, reservoirSize, eventsSeen, errorEvents);
    }

    @Override
    public <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<T> events) throws Exception {
        sendAnalyticEventsForReservoir(CollectorMethods.ANALYTIC_EVENT_DATA, compressedEncoding, reservoirSize, eventsSeen, events);
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
        sendAnalyticEventsForReservoir(CollectorMethods.CUSTOM_EVENT_DATA, compressedEncoding, reservoirSize, eventsSeen, events);
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
        sendLogEventsForReservoir(CollectorMethods.LOG_EVENT_DATA, compressedEncoding, events);
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, Collection<SpanEvent> events) throws Exception {
        sendAnalyticEventsForReservoir(CollectorMethods.SPAN_EVENT_DATA, compressedEncoding, reservoirSize, eventsSeen, events);
    }

    private <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticEventsForReservoir(String method, String encoding, int reservoirSize, int eventsSeen,
            Collection<T> events) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || events.isEmpty()) {
            return;
        }
        InitialSizedJsonArray params = new InitialSizedJsonArray(3);
        params.add(runId);

        JSONObject metadata = new JSONObject();
        metadata.put("reservoir_size", reservoirSize);
        metadata.put("events_seen", eventsSeen);
        params.add(metadata);

        params.add(events);
        invokeRunId(method, encoding, runId, params);
    }

    // Sends LogEvent data in the MELT format for logs
    // https://docs.newrelic.com/docs/logs/log-api/introduction-log-api/#log-attribute-example
    private <T extends AnalyticsEvent & JSONStreamAware> void sendLogEventsForReservoir(String method, String encoding, Collection<T> events) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || events.isEmpty()) {
            return;
        }

        JSONObject commonAttributes = new JSONObject();

        // build attributes object
        JSONObject attributes = new JSONObject();
        attributes.put("attributes", commonAttributes);

        // build common object
        JSONObject common = new JSONObject();
        common.put("common", attributes);

        // build logs object
        JSONObject logs = new JSONObject();
        logs.put("logs", events);

        // params is top level
        InitialSizedJsonArray params = new InitialSizedJsonArray(3);
        params.add(common);
        params.add(logs);
        invokeRunId(method, encoding, runId, params);
    }

    @Override
    public void sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || metricData.isEmpty()) {
            return;
        }

        InitialSizedJsonArray params = new InitialSizedJsonArray(4);
        params.add(runId);
        params.add(beginTimeMillis / 1000);
        params.add(endTimeMillis / 1000);
        params.add(metricData);

        invokeRunId(CollectorMethods.METRIC_DATA, compressedEncoding, runId, params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || profiles.isEmpty()) {
            return Collections.emptyList();
        }

        InitialSizedJsonArray params = new InitialSizedJsonArray(2);
        params.add(runId);
        params.add(profiles);

        Object response = invokeRunId(CollectorMethods.PROFILE_DATA, getEncodingForComplexCompression(), runId, params);
        if (response == null || NULL_RESPONSE.equals(response)) {
            return Collections.emptyList();
        }
        try {
            return (List<Long>) response;
        } catch (ClassCastException e) {
            logger.warning(MessageFormat.format("Invalid response from New Relic sending profiles: {0}", e));
            throw e;
        }
    }

    /**
     * Sends the jars with versions to the collector.
     *
     * @param jarDataList The new jars which need to be sent to the collector.
     */
    @Override
    public void sendModules(List<? extends JSONStreamAware> jarDataList) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || jarDataList == null || jarDataList.isEmpty()) {
            return;
        }
        InitialSizedJsonArray params = new InitialSizedJsonArray(2);

        // Module type must always be first - it should always be jars
        params.add(MODULE_TYPE);
        params.add(jarDataList);

        invokeRunId(CollectorMethods.UPDATE_LOADED_MODULES, compressedEncoding, runId, params);
    }

    /**
     * Some of our data calls are json documents of base 64 encoded strings with gzipped json docs inside of them.
     * We normally send these requests with IDENTITY encoding because a large portion of the payload is already compressed.
     * When the "simple_compression" flag is on, we directly include the json docs instead of compressing them, and we
     * DEFLATE the entire json document instead.
     *
     * @return the type of encoding to use based on the simple_compression configuration value
     */
    private String getEncodingForComplexCompression() {
        return configService.getDefaultAgentConfig().isSimpleCompression() ? compressedEncoding : IDENTITY_ENCODING;
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || sqlTraces.isEmpty()) {
            return;
        }

        InitialSizedJsonArray params = new InitialSizedJsonArray(1);
        params.add(sqlTraces);

        invokeRunId(CollectorMethods.SQL_TRACE_DATA, getEncodingForComplexCompression(), runId, params);
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID || traces.isEmpty()) {
            return;
        }

        InitialSizedJsonArray params = new InitialSizedJsonArray(2);
        params.add(runId);
        params.add(traces);

        invokeRunId(CollectorMethods.TRANSACTION_SAMPLE_DATA, getEncodingForComplexCompression(), runId, params);
    }

    // The fix for JAVA-2965 assumes RPMService.shutdown() is the only caller of this method.
    // There's no way to avoid this bogus assumption short of a major rewrite of this layer.
    @Override
    public void shutdown(long timeMillis) throws Exception {
        Object runId = agentRunId;
        if (runId == NO_AGENT_RUN_ID) {
            return;
        }

        InitialSizedJsonArray params = new InitialSizedJsonArray(2);
        params.add(runId);
        params.add(timeMillis);
        try {
            invokeRunId(CollectorMethods.SHUTDOWN, compressedEncoding, runId, params);
        } finally {
            setAgentRunId(NO_AGENT_RUN_ID);
            this.httpClientWrapper.shutdown();
        }
    }

    @VisibleForTesting
    void setMaxPayloadSizeInBytes(int payloadSizeInBytes) {
        maxPayloadSizeInBytes = payloadSizeInBytes;
    }

    private Object invokeRunId(String method, String encoding, Object runId, JSONStreamAware params) throws Exception {
        String uri = MessageFormat.format(agentRunIdUriPattern, method, runId.toString());
        return invoke(redirectHost, method, encoding, uri, params);
    }

    private Object invokeNoRunId(String host, String method, String encoding, JSONStreamAware params) throws Exception {
        String uri = MessageFormat.format(noAgentRunIdUriPattern, method);
        return invoke(host, method, encoding, uri, params);
    }

    private Object invoke(String host, String method, String encoding, String uri, JSONStreamAware params) throws Exception {
        // ReadResult should be from a valid 2xx response at this point otherwise send method throws an exception here
        ReadResult readResult = send(host, method, encoding, uri, params);
        Map<?, ?> responseMap = null;
        String responseBody = readResult.getResponseBody();

        if (responseBody != null && !responseBody.isEmpty()) {
            try {
                responseMap = getResponseMap(responseBody);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error parsing response JSON({0}) from NewRelic: {1}", method, e.toString());
                logger.log(Level.FINEST, "Invalid response JSON({0}): {1}", method, responseBody);
                throw e;
            }
        } else if (METHODS_WITH_RESPONSE_BODY.contains(method)) {
            // Only log this if it's a method that we would expect to have data in the response payload
            logger.log(Level.FINER, "Response was null ({0})", method);
        }

        if (responseMap != null) {
            if (dataSenderListener != null) {
                dataSenderListener.dataReceived(method, encoding, uri, responseMap);
            }

            try {
                return responseMap.get(EXCEPTION_MAP_RETURN_VALUE_KEY);
            } catch (ClassCastException ex) {
                logger.log(Level.WARNING, "Error parsing response JSON({0}) from NewRelic: {1}", method, ex.toString());
                return null;
            }
        } else {
            return null;
        }
    }

    /*
     * As of Protocol 17 agents MUST NOT depend on the content of the response body for any behavior; just the integer
     * response code value. The previous behavior of a 200 ("OK") with an exact string in the body that should be
     * matched/parsed has been deprecated.
     */
    private ReadResult connectAndSend(String host, String method, String encoding, String uri, JSONStreamAware params) throws Exception {
        byte[] data = writeData(encoding, params);

        /*
         * We don't enforce max_payload_size_in_bytes for error_data (aka error traces). Instead, we halve the
         * payload and try again. See RPMService sendErrorData
         */
        if (data.length > maxPayloadSizeInBytes && !method.equals(CollectorMethods.ERROR_DATA)) {
            ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(
                    MessageFormat.format(MetricNames.SUPPORTABILITY_PAYLOAD_SIZE_EXCEEDS_MAX, method), 1), MetricNames.SUPPORTABILITY_PAYLOAD_SIZE_EXCEEDS_MAX);
            String msg = MessageFormat.format("Payload of size {0} exceeded maximum size {1} for {2} method ",
                    data.length, maxPayloadSizeInBytes, method);
            logger.log(Level.WARNING, msg);
            throw new MaxPayloadException(msg);
        }

        final URL url = new URL(PROTOCOL, host, port, uri);
        HttpClientWrapper.Request request = createRequest(method, encoding, url, data);

        httpClientWrapper.captureSupportabilityMetrics(ServiceFactory.getStatsService(), host);

        ReadResult result = httpClientWrapper.execute(request, new TimingEventHandler(method, ServiceFactory.getStatsService()));

        String payloadJsonSent = DataSenderWriter.toJSONString(params);

        if (auditMode && methodShouldBeAudited(method)) {

            String msg = MessageFormat.format("Sent JSON({0}) to: {1}, with payload: {2}", method, obfuscateLicenseKey(url.toString()), obfuscateLicenseKey(payloadJsonSent));
            logger.info(msg);
        }

        // Create supportability metric for all response codes
        ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(
                MessageFormat.format(MetricNames.SUPPORTABILITY_HTTP_CODE, result.getStatusCode()), 1), MetricNames.SUPPORTABILITY_HTTP_CODE);

        if (result.getStatusCode() != HttpResponseCode.OK && result.getStatusCode() != HttpResponseCode.ACCEPTED) {
            throwExceptionFromStatusCode(method, result, data, request);
        }

        String payloadJsonReceived = result.getResponseBody();

        // received successful 2xx response
        if (auditMode && methodShouldBeAudited(method)) {
            logger.info(MessageFormat.format("Received JSON({0}): {1}", method, payloadJsonReceived));
        }

        recordDataUsageMetrics(method, payloadJsonSent, payloadJsonReceived);

        AgentControlIntegrationUtils.reportHealthyStatus(healthDataChangeListeners, AgentHealth.Category.HARVEST, AgentHealth.Category.CONFIG);
        if (method.equals(CollectorMethods.CONNECT)) {
            AgentControlIntegrationUtils.assignEntityGuid(healthDataChangeListeners, payloadJsonReceived);
        }

        if (dataSenderListener != null) {
            dataSenderListener.dataSent(method, encoding, uri, data);
        }

        return result;
    }

    /**
     * Record metrics tracking amount of bytes sent and received for each agent endpoint payload
     *
     * @param method method for the agent endpoint
     * @param payloadJsonSent JSON String of the payload that was sent
     * @param payloadJsonReceived JSON String of the payload that was received
     */
    private void recordDataUsageMetrics(String method, String payloadJsonSent, String payloadJsonReceived) {
        int payloadBytesSent = payloadJsonSent.getBytes().length;
        int payloadBytesReceived = payloadJsonReceived.getBytes().length;

        // COLLECTOR is always the destination for data reported via DataSenderImpl.
        // OTLP as a destination is not currently supported by the Java agent.
        // INFINITE_TRACING destined usage data is sent via SpanEventSender.
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordDataUsageMetricWork(
                        MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES, COLLECTOR),
                        payloadBytesSent, payloadBytesReceived), MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES + " " + COLLECTOR);

        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordDataUsageMetricWork(
                        MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES, COLLECTOR, method),
                        payloadBytesSent, payloadBytesReceived),
                        MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES + " " + COLLECTOR);
    }

    private void throwExceptionFromStatusCode(String method, ReadResult result, byte[] data, HttpClientWrapper.Request request)
            throws HttpError, LicenseException, ForceRestartException, ForceDisconnectException {
        // Comply with spec and send supportability metric only for error responses
        ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(
                MessageFormat.format(MetricNames.SUPPORTABILITY_AGENT_ENDPOINT_HTTP_ERROR, result.getStatusCode()), 1), MetricNames.SUPPORTABILITY_AGENT_ENDPOINT_HTTP_ERROR);
        ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(
                MessageFormat.format(MetricNames.SUPPORTABILITY_AGENT_ENDPOINT_ATTEMPTS, method), 1), MetricNames.SUPPORTABILITY_AGENT_ENDPOINT_ATTEMPTS);

        // HttpError exceptions are typically handled in RPMService or the harvestable service for the requested endpoint
        switch (result.getStatusCode()) {
            case HttpResponseCode.PROXY_AUTHENTICATION_REQUIRED:
                // agent receives a 407 response due to a misconfigured proxy (not from NR backend), throw exception
                AgentControlIntegrationUtils.reportUnhealthyStatus(healthDataChangeListeners, AgentHealth.Status.PROXY_ERROR,
                        Integer.toString(result.getStatusCode()), method);
                final String authField = result.getProxyAuthenticateHeader();
                if (authField != null) {
                    throw new HttpError("Proxy Authentication Mechanism Failed: " + authField, result.getStatusCode(), data.length);
                } else {
                    throw new HttpError("Proxy Authentication Mechanism Failed: " + "null Proxy-Authenticate header", result.getStatusCode(), data.length);
                }
            case HttpResponseCode.UNAUTHORIZED:
                // received 401 Unauthorized, throw exception instead of parsing LicenseException from 200 response body
                AgentControlIntegrationUtils.reportUnhealthyStatus(healthDataChangeListeners, AgentHealth.Status.INVALID_LICENSE);
                throw new LicenseException(parseExceptionMessage(result.getResponseBody()));
            case HttpResponseCode.CONFLICT:
                // received 409 Conflict, throw exception instead of parsing ForceRestartException from 200 response body
                throw new ForceRestartException(parseExceptionMessage(result.getResponseBody()));
            case HttpResponseCode.GONE:
                // received 410 Gone, throw exception instead of parsing ForceDisconnectException from 200 response body
                AgentControlIntegrationUtils.reportUnhealthyStatus(healthDataChangeListeners, AgentHealth.Status.FORCED_DISCONNECT);
                throw new ForceDisconnectException(parseExceptionMessage(result.getResponseBody()));
            default:
                // response is bad (neither 200 nor 202), throw generic HttpError exception
                AgentControlIntegrationUtils.reportUnhealthyStatus(healthDataChangeListeners, AgentHealth.Status.HTTP_ERROR,
                        Integer.toString(result.getStatusCode()), method);
                logger.log(Level.FINER, "Connection http status code: {0}", result.getStatusCode());
                throw HttpError.create(result.getStatusCode(), request.getURL().getHost(), data.length);
        }
    }

    private String parseExceptionMessage(String responseBody) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject responseMessageObject = (JSONObject) parser.parse(responseBody);
            JSONObject exception = (JSONObject) responseMessageObject.get("exception");
            return exception.get("message").toString();
        } catch (Exception ignored) {
            return responseBody;
        }
    }

    private boolean methodShouldBeAudited(String method) {
        if (auditModeEndpoints != null && auditModeEndpoints.size() > 0) {
            return auditModeEndpoints.contains(method);
        }
        return true;
    }

    private ReadResult send(String host, String method, String encoding, String uri, JSONStreamAware params) throws Exception {
        try {
            return connectAndSend(host, method, encoding, uri, params);
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "You have requested a connection to New Relic via a protocol which is unavailable in your runtime: {0}", e.toString());
            throw new ForceDisconnectException(e.toString());
        } catch (SocketException e) {
            if (e.getCause() instanceof NoSuchAlgorithmException) {
                String msg = MessageFormat.format("You have requested a connection to New Relic via an algorithm which is unavailable in your runtime: {0}."
                        + " This may also be indicative of a corrupted keystore or trust store on your server.", e.getCause().toString());
                logger.error(msg);
                // this is a recoverable error. Try again later
            } else {
                logger.log(Level.SEVERE, "A socket exception was encountered while sending data to New Relic ({0})."
                        + " Please check your network / proxy settings.", e.toString());
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Error sending JSON({0}): {1}", method, DataSenderWriter.toJSONString(params));
                }
                logger.log(Level.FINEST, e, e.toString());
            }
            throw e;
        } catch (HttpError e) {
            // These errors are logged upstream of this call.
            throw e;
        } catch (Exception e) {
            if (e instanceof SSLHandshakeException) {
                logger.log(Level.SEVERE, "Unable to connect to New Relic due to an SSL error."
                        + " Consider enabling -Djavax.net.debug=all to debug your SSL configuration such as your trust store.", e);
            }
            logger.log(Level.SEVERE, "Remote {0} call failed : {1}.", method, e.toString());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Error sending JSON({0}): {1}", method, DataSenderWriter.toJSONString(params));
            }
            logger.log(Level.FINEST, e, e.toString());
            throw e;
        }
    }

    private HttpClientWrapper.Request createRequest(String method, String encoding, URL url, byte[] data) {
        final boolean isConnectOrPreconnect = method.equals(CollectorMethods.CONNECT) || method.equals(CollectorMethods.PRECONNECT);
        final Map<String, String> requestMetadata = (this.requestMetadata != null && !isConnectOrPreconnect)
                ? this.requestMetadata
                : Collections.<String, String>emptyMap();

        return new HttpClientWrapper.Request()
                .setURL(url)
                .setVerb(putForDataSend ? HttpClientWrapper.Verb.PUT : HttpClientWrapper.Verb.POST)
                .setEncoding(encoding)
                .setData(data)
                .setRequestMetadata(requestMetadata);
    }

    private byte[] writeData(String encoding, JSONStreamAware params) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try (
                OutputStream os = getOutputStream(outStream, encoding);
                Writer out = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        ) {
            JSONValue.writeJSONString(params, out);
            out.flush();
        }
        return outStream.toByteArray();
    }

    private OutputStream getOutputStream(OutputStream out, String encoding) throws IOException {
        if (DEFLATE_ENCODING.equals(encoding)) {
            return new DeflaterOutputStream(out, new Deflater(COMPRESSION_LEVEL));
        } else if (GZIP_ENCODING.equals(encoding)) {
            return new GZIPOutputStream(out);
        } else {
            return out;
        }
    }

    private Map<?, ?> getResponseMap(String responseBody) throws Exception {
        JSONParser parser = new JSONParser();
        Object response = parser.parse(responseBody);
        return (Map<?, ?>) response;
    }

    private static class TimingEventHandler implements HttpClientWrapper.ExecuteEventHandler {
        private final String method;
        private final StatsService statsService;
        private long requestSent;

        TimingEventHandler(String method, StatsService statsService) {
            this.method = method;
            this.statsService = statsService;
        }

        @Override
        public void requestStarted() {
            requestSent = System.currentTimeMillis();
        }

        @Override
        public void requestEnded() {
            long requestDuration = System.currentTimeMillis() - requestSent;

            statsService.doStatsWork(StatsWorks.getRecordResponseTimeWork(
                    MessageFormat.format(MetricNames.SUPPORTABILITY_AGENT_ENDPOINT_DURATION, method), requestDuration),
                    MetricNames.SUPPORTABILITY_AGENT_ENDPOINT_DURATION + " " + method);
        }
    }

    @Override
    public void registerHealthDataChangeListener(HealthDataChangeListener listener) {
        healthDataChangeListeners.add(listener);
    }
}
