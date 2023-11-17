/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.tracers.CrossProcessNameFormat;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.agent.util.TimeConversion;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Cross process tracing support. Terminology: the request that triggered this transaction is known as the "inbound
 * request". This class relies on the {@link InboundHeaderState} class for inbound request processing, if any (not all
 * transactions are initiated by requests). The eventual response to the inbound request is called the outbound
 * response. In the mean time, this transaction may make one or more external service calls. Associated with each of
 * these calls is one outbound request and one inbound response.
 * <p>
 * This class is thread safe. It shares a lock with its owning Transaction to eliminate the possibility of deadlock.
 */
public class CrossProcessTransactionStateImpl implements CrossProcessTransactionState {

    private static final boolean OPTIMISTIC_TRACING = false;
    private static final int ALTERNATE_PATH_HASH_MAX_COUNT = 10;
    private static final String UNKNOWN_HOST = "Unknown";

    private static final Set<String> UNOBFUSCATED_HEADERS = Sets.newHashSet(HeadersUtil.NEWRELIC_TRACE_HEADER,
            HeadersUtil.NEWRELIC_TRACE_MESSAGE_HEADER, HeadersUtil.W3C_TRACEPARENT_HEADER, HeadersUtil.W3C_TRACESTATE_HEADER);

    // Threading: this object interacts heavily with the transaction, and control flow moves in both directions between
    // the objects. If this object defined its own lock, a high likelihood of deadlocks would exist during future code
    // evolution. Instead, this object shares the lock of its owning transaction.
    // The lock is used (1) to serialize methods that touch processOutboundResponseDone, (2) to ensure that at most one
    // guid and one trip ID are created per transaction, and (3) to serialize access to alternatePathHashes.

    private final Transaction tx;
    private final Object lock;

    // State: this object works closely with the immutable InboundHeaderState. Mutable state lazily created or modified
    // during a CAT transaction is maintained here. TripId in particular is tricky. If we are responding to a CAT
    // request,
    // the tripId from the InboundHeaderState becomes our trip ID (lazily). If not, but we originate at least one CAT
    // request, then the GUID of our owning transaction becomes our trip ID (and thus becomes the trip ID of
    // transactions
    // we invoke on remote applications, if they are instrumented with New Relic).

    private volatile String tripId;
    private volatile boolean isCatOriginator = false;
    private final Set<String> alternatePathHashes;

    // This flag is used to ensure we only perform outbound response header processing once. It is guarded by the lock.

    private volatile boolean processOutboundResponseDone = false;

    private CrossProcessTransactionStateImpl(Transaction tx) {
        this.tx = tx;
        this.lock = tx.getLock();
        // does not permit null keys or values
        MapMaker factory = new MapMaker().initialCapacity(8).concurrencyLevel(4);
        alternatePathHashes = Sets.newSetFromMap(new LazyMapImpl<String, Boolean>(factory));
    }

    @Override
    public void writeResponseHeaders() {
        if (tx.isIgnore()) {
            return;
        }
        Dispatcher dispatcher = tx.getDispatcher();
        if (dispatcher == null) {
            return;
        }
        try {
            Response response = dispatcher.getResponse();
            long contentLength = -1;
            if (response instanceof ExtendedResponse) {
                contentLength = ((ExtendedResponse) response).getContentLength();
            } else {
                contentLength = tx.getInboundHeaderState().getRequestContentLength();
            }
            processOutboundResponseHeaders(response, contentLength);
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINEST, e, "Error attempting to write response headers in transaction: {0}", tx);
        }
    }

    @Override
    public void processOutboundResponseHeaders(OutboundHeaders outboundHeaders, long contentLength) {
        if (outboundHeaders != null) {
            tx.getTransactionActivity().markAsResponseSender();
            OutboundHeadersMap metadata = new OutboundHeadersMap(outboundHeaders.getHeaderType());
            boolean populated = populateResponseMetadata(metadata, contentLength);

            if (populated && obfuscateMetadata(metadata)) {
                for (Entry<String, String> entry : metadata.entrySet()) {
                    outboundHeaders.setHeader(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private boolean obfuscateMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }

        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        if (encodingKey == null) {
            Agent.LOG.log(Level.FINER, "Metadata obfuscation failed. Encoding key is null");
            return false;
        }

        for (Entry<String, String> entry : metadata.entrySet()) {
            if (UNOBFUSCATED_HEADERS.contains(entry.getKey())) {
                continue;
            }
            String obfuscatedValue = Obfuscator.obfuscateNameUsingKey(entry.getValue(), encodingKey);
            entry.setValue(obfuscatedValue);
        }
        return true;
    }

    /**
     * Populates CAT metadata for an outbound response.
     *
     * @return true if metadata was populated.
     */
    private boolean populateResponseMetadata(OutboundHeaders headers, long contentLength) {
        if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            Agent.LOG.log(Level.FINEST, "Distributed tracing enabled. Not adding response metadata");
            return false;
        } else if (tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            synchronized (lock) { // the lock guards processOutboundResponseDone
                if (processOutboundResponseDone) {
                    return false;
                }

                if (tx.isIgnore()) {
                    Agent.LOG.log(Level.FINEST,
                            "Not adding response headers in transaction {0}. Transaction is ignored", tx);
                    return false;
                }

                if (!tx.getInboundHeaderState().isTrustedCatRequest()) {
                    Agent.LOG.log(Level.FINEST,
                            "Not adding response headers in transaction {0}. Not a trusted CAT request", tx);
                    return false;
                }

                // this has the side-effect of possibly ignoring the transaction
                tx.freezeTransactionName();

                long durationInNanos = tx.getRunningDurationInNanos();
                recordClientApplicationMetric(durationInNanos);

                writeCrossProcessAppDataResponseHeader(headers, durationInNanos, contentLength);
                processOutboundResponseDone = true;
            }

            return true;
        } else {
            Agent.LOG.log(Level.FINEST,
                    "Not adding response headers in transaction {0}. Neither distributed tracing nor CAT are enabled", tx);
            return false;
        }
    }

    /**
     * Outgoing service request initiated by this transaction.
     */
    @Override
    public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        com.newrelic.api.agent.TracedMethod lastTracer = NewRelic.getAgent().getTracedMethod();
        processOutboundRequestHeaders(outboundHeaders, lastTracer);
    }

    @Override
    public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders, com.newrelic.api.agent.TracedMethod tracedMethod) {
        if (outboundHeaders != null) {
            OutboundHeadersMap metadata = new OutboundHeadersMap(outboundHeaders.getHeaderType());
            populateRequestMetadata(metadata, tracedMethod);

            if (obfuscateMetadata(metadata)) {
                for (Entry<String, String> entry : metadata.entrySet()) {
                    outboundHeaders.setHeader(entry.getKey(), entry.getValue());
                }
            } else {
                // If we got in here, old CAT is disabled but DT might be
                // enabled, so we should try to set those unobfuscated headers
                for (Entry<String, String> entry : metadata.entrySet()) {
                    if (UNOBFUSCATED_HEADERS.contains(entry.getKey())) {
                        outboundHeaders.setHeader(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    public void populateRequestMetadata(OutboundHeaders headers, com.newrelic.api.agent.TracedMethod tracedMethod) {
        // Synthetics is not disabled when CAT is disabled, so we must check this first.
        if (tx.getInboundHeaderState().isTrustedSyntheticsRequest()) {
            if (tx.isInProgress() && !tx.isIgnore()) {
                String synHeader = tx.getInboundHeaderState().getUnparsedSyntheticsHeader();
                if (synHeader != null) {
                    HeadersUtil.setSyntheticsHeader(headers, synHeader);
                }
                String synInfoHeader = tx.getInboundHeaderState().getUnparsedSyntheticsInfoHeader();
                if (synInfoHeader != null) {
                    HeadersUtil.setSyntheticsInfoHeader(headers, synInfoHeader);
                }
            }
        }

        // Enabling distributed tracing disables old CAT.
        if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            this.isCatOriginator = HeadersUtil.createAndSetDistributedTraceHeaders(tx, tracedMethod, headers);
        } else if (tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            synchronized (lock) {
                if (tx.getDispatcher() == null) {
                    Agent.LOG.log(Level.FINEST,
                            "Dispatcher not set. Not setting CAT request headers in transaction {0}", tx);
                }

                if (tx.isIgnore()) {
                    Agent.LOG.log(Level.FINEST,
                            "Transaction ignored. Not setting CAT request headers in transaction {0}", tx);
                    return;
                }

                String crossProcessId = tx.getCrossProcessConfig().getEncodedCrossProcessId();
                if (crossProcessId != null) {
                    Agent.LOG.log(Level.FINER, "Sending ID header: {0} in transaction {1}", crossProcessId, tx);
                    this.isCatOriginator = true;
                    HeadersUtil.setIdHeader(headers, tx.getCrossProcessConfig().getCrossProcessId());
                    String transactionHeaderValue = getTransactionHeaderValue();
                    HeadersUtil.setTransactionHeader(headers, transactionHeaderValue);
                } else {
                    Agent.LOG.log(Level.FINEST,
                            "Encoded cross process id is null. Not setting CAT request headers in transaction {0}", tx);
                }
            }
        }
    }

    private void doProcessInboundResponseHeaders(TracedMethod tracer, CrossProcessNameFormat crossProcessFormat,
            String host, boolean addRollupMetrics) {

        if (crossProcessFormat != null) {
            if (tracer instanceof DefaultTracer) {
                DefaultTracer dt = (DefaultTracer) tracer;
                String transactionId = crossProcessFormat.getTransactionId();
                if (transactionId != null && transactionId.length() > 0) {
                    dt.setAgentAttribute(AttributeNames.TRANSACTION_TRACE_ID_PARAMETER_NAME, transactionId);
                }
                dt.setMetricNameFormat(crossProcessFormat);
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.log(Level.FINEST,
                            "Received APP_DATA cross process response header for external call: {0} in transaction {1}",
                            crossProcessFormat.toString(), tx);
                }
            }
            if (addRollupMetrics && !UNKNOWN_HOST.equals(host)) {
                tracer.addRollupMetricName(crossProcessFormat.getHostCrossProcessIdRollupMetricName());
            }
        }

        if (addRollupMetrics) {
            recordExternalMetrics(tracer, host);
        }
    }

    private void recordExternalMetrics(TracedMethod tracer, String host) {
        tracer.addRollupMetricName(MetricNames.EXTERNAL_PATH, host, "all");
        tracer.addRollupMetricName(MetricNames.EXTERNAL_ALL);
        if (tx != null) {
            if (tx.getDispatcher().isWebTransaction()) {
                tracer.addRollupMetricName(MetricNames.WEB_TRANSACTION_EXTERNAL_ALL);
            } else {
                tracer.addRollupMetricName(MetricNames.OTHER_TRANSACTION_EXTERNAL_ALL);
            }
        }
    }

    /**
     * Processing for inbound response to a previously-issue external request.
     */
    @Override
    public void processInboundResponseHeaders(InboundHeaders inboundHeaders, TracedMethod tracer, String host,
            String uri, boolean addRollupMetrics) {
        if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            if (addRollupMetrics) {
                recordExternalMetrics(tracer, host);
            }
        } else if (tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            if (inboundHeaders == null || tracer == null) {
                return;
            }

            String encodedAppData = HeadersUtil.getAppDataHeader(inboundHeaders);
            String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
            CrossProcessNameFormat crossProcessFormat = CrossProcessNameFormat.create(host, uri, encodedAppData,
                    encodingKey);

            if (crossProcessFormat == null) {
                Agent.LOG.log(Level.FINER, "Unable to decode Application Data in transaction {0}", tx);
            }

            doProcessInboundResponseHeaders(tracer, crossProcessFormat, host, addRollupMetrics);
        }
    }

    /* package visibility only, and even that only for testing */
    synchronized String getTransactionHeaderValue() {
        synchronized (lock) {
            String json = getTransactionHeaderJson(tx.getGuid(), getForceTransactionTrace(), getTripId(),
                    generatePathHash());
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Sending TRANSACTION header: {0}", json);
            }

            return json;
        }
    }

    /**
     * Get a JSON string containing the value of the transaction header
     */
    private String getTransactionHeaderJson(String guid, boolean forceTransactionTrace, String trip, int pathHash) {
        List<?> args = Arrays.asList(guid, forceTransactionTrace, trip, ServiceUtils.intToHexString(pathHash));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            JSONArray.writeJSONString(args, writer);
            writer.close();
            return out.toString("UTF-8");
        } catch (IOException e) {
            Agent.LOG.error(MessageFormat.format("Error getting JSON: {0}", e));
            return null;
        }
    }

    /**
     * Write the cross process app data to a response header.
     */
    private void writeCrossProcessAppDataResponseHeader(OutboundHeaders headers, long durationInNanos,
            long contentLength) {
        String json = getCrossProcessAppDataJson(durationInNanos, contentLength);
        if (json == null) {
            return;
        }
        Agent.LOG.log(Level.FINEST, "Setting APP_DATA response header in transaction {0} to: {1}", tx, json);
        HeadersUtil.setAppDataHeader(headers, json);
    }

    /**
     * Get a JSON string containing the cross application data.
     */
    private String getCrossProcessAppDataJson(long durationInNanos, long contentLength) {
        String crossProcessId = tx.getCrossProcessConfig().getCrossProcessId();
        String transactionName = tx.getPriorityTransactionName().getName();
        Float queueTimeInSeconds = (float) tx.getExternalTime() / TimeConversion.MILLISECONDS_PER_SECOND;
        Float durationInSeconds = (float) durationInNanos / TimeConversion.NANOSECONDS_PER_SECOND;
        List<?> args = Arrays.asList(crossProcessId, transactionName, queueTimeInSeconds, durationInSeconds,
                contentLength, tx.getGuid(), Boolean.FALSE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            JSONArray.writeJSONString(args, writer);
            writer.close();
            return out.toString();
        } catch (IOException e) {
            Agent.LOG.error(MessageFormat.format("Error getting JSON {0} in transaction {1}", e, tx));
            return null;
        }
    }

    // Record the ClientApplication metric. The UI uses this to create the Application Map.
    private void recordClientApplicationMetric(long durationInNanos) {
        if (tx.getInboundHeaderState().isTrustedCatRequest()) {
            String metricName = MessageFormat.format(MetricNames.CLIENT_APPLICATION_FORMAT,
                    tx.getInboundHeaderState().getClientCrossProcessId());
            tx.getTransactionActivity()
                    .getTransactionStats()
                    .getUnscopedStats()
                    .getOrCreateResponseTimeStats(metricName)
                    .recordResponseTime(durationInNanos, TimeUnit.NANOSECONDS);
            Agent.LOG.log(Level.FINEST, "Recorded ClientApplication metric in transaction {0}", tx);
        }
    }

    private boolean getForceTransactionTrace() {
        return OPTIMISTIC_TRACING;
    }

    /**
     * Get the trip ID. The trip ID is non-null if this object's owning Transaction is a "CAT participant". We are CAT
     * participant if either (1) the inbound request that initiated us contained a valid CAT header, or (2) we are a
     * "CAT originator", meaning we have originated at least one CAT transaction that instrumentation was able to
     * detect.
     *
     * @return the trip ID if we are a CAT participant or null if we aren't. Note that the return value can change
     * during the course of a transaction.
     */
    @Override
    public String getTripId() {
        AgentConfig agentConfig = tx.getAgentConfig();
        DistributedTracingConfig config = agentConfig.getDistributedTracingConfig();
        if (!config.isEnabled() && !tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return null;
        }
        if (tripId == null) {
            tripId = tx.getInboundHeaderState().getInboundTripId();
        }
        if (tripId == null && tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            tripId = tx.getSpanProxy().getTraceId();
        }
        if (tripId == null && this.isCatOriginator) {
            tripId = tx.getGuid(); // never null
        }
        return tripId;
    }

    @Override
    public int generatePathHash() {
        synchronized (lock) {
            int pathHash = ServiceUtils.calculatePathHash(tx.getApplicationName(),
                    tx.getPriorityTransactionName().getName(), tx.getInboundHeaderState().getReferringPathHash());

            if (alternatePathHashes.size() < ALTERNATE_PATH_HASH_MAX_COUNT) {
                alternatePathHashes.add(ServiceUtils.intToHexString(pathHash));
            }
            return pathHash;
        }
    }

    @Override
    public String getAlternatePathHashes() {
        if (!tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return null;
        }

        synchronized (lock) {
            Set<String> hashes = new TreeSet<>(alternatePathHashes);
            hashes.remove(ServiceUtils.intToHexString(generatePathHash()));
            StringBuilder result = new StringBuilder();
            for (String alternatePathHash : hashes) {
                result.append(alternatePathHash);
                result.append(",");
            }
            return result.length() > 0 ? result.substring(0, result.length() - 1) : null;
        }
    }

    public static CrossProcessTransactionStateImpl create(Transaction tx) {
        return tx == null ? null : new CrossProcessTransactionStateImpl(tx);
    }

    @Override
    public String getRequestMetadata() {
        if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            Agent.LOG.log(Level.FINEST, "Distributed tracing is enabled. Ignoring getRequestMetadata call.");
            return null;
        }

        OutboundHeadersMap metadata = new OutboundHeadersMap(HeaderType.MESSAGE);
        populateRequestMetadata(metadata, tx.getTransactionActivity().getLastTracer());

        // CAT specification: return null when there are no headers to send.
        if (metadata.isEmpty()) {
            return null;
        }

        // JSON serialize, obfuscate, and Base64 encode
        String serializedMetadata = JSONValue.toJSONString(metadata);
        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        return Obfuscator.obfuscateNameUsingKey(serializedMetadata, encodingKey);
    }

    @Override
    public void processRequestMetadata(String requestMetadata) {
        if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            Agent.LOG.log(Level.FINEST, "Distributed tracing is enabled. Ignoring processRequestMetadata call.");
            return;
        }
        InboundHeaders headers = decodeMetadata(requestMetadata);
        Transaction currentTxn = Transaction.getTransaction(false);
        if (currentTxn != null) {
            currentTxn.provideRawHeaders(headers);
        }
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_PROCESS_REQUEST_METADATA);
    }

    @Override
    public String getResponseMetadata() {
        if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            Agent.LOG.log(Level.FINEST, "Distributed tracing is enabled. Ignoring getResponseMetadata call.");
            return null;
        }

        // The request content length should be equal to the request header Content-Length, or -1 if that header does
        // not exist. See https://newrelic.atlassian.net/wiki/display/eng/Cross+Process+metrics.
        OutboundHeadersMap metadata = new OutboundHeadersMap(HeaderType.MESSAGE);
        populateResponseMetadata(metadata, -1);

        // CAT specification: return null when there are no headers to send.
        if (metadata.isEmpty()) {
            return null;
        }

        // JSON serialize, obfuscate, and Base64 encode
        String serializedMetadata = JSONValue.toJSONString(metadata);
        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        return Obfuscator.obfuscateNameUsingKey(serializedMetadata, encodingKey);
    }

    @Override
    public void processResponseMetadata(String responseMetadata, URI uri) {
        if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            Agent.LOG.log(Level.FINEST, "Distributed tracing is enabled. Ignoring processResponseMetadata call.");
            return;
        }

        if (!tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return;
        }

        if (responseMetadata == null) {
            return;
        }

        Tracer lastTracer = tx.getTransactionActivity().getLastTracer();
        if (lastTracer == null) {
            return;
        }

        InboundHeaders NRHeaders = decodeMetadata(responseMetadata);
        if (NRHeaders != null) {

            /*
              One of our public APIs doesn't have a URI parameter, so we may not know the host or URI.

              See {@link com.newrelic.api.agent.Transaction#processResponseMetadata(String)}
             */
            String host = (uri == null || uri.getHost() == null) ? UNKNOWN_HOST : uri.getHost();
            String uriString = (uri == null) ? null : uri.toString();

            String decodedAppData = HeadersUtil.getAppDataHeader(NRHeaders);

            CrossProcessNameFormat crossProcessFormat = CrossProcessNameFormat.create(host, uriString, decodedAppData);
            doProcessInboundResponseHeaders(lastTracer, crossProcessFormat, host, true);

            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_PROCESS_RESPONSE_METADATA);
        }
    }

    /**
     * @param metadata
     * @return decoded InboundHeaders. Returns null if decoding or parsing failed, or if there are no headers to parse.
     */
    private InboundHeaders decodeMetadata(String metadata) {
        // Deobfuscate and deserialize.
        String deobfuscatedMetadata;
        String encodingKey = tx.getCrossProcessConfig().getEncodingKey();
        // RPM service not connected yet.
        if (encodingKey == null) {
            return null;
        }

        deobfuscatedMetadata = Obfuscator.deobfuscateNameUsingKey(metadata, encodingKey);

        Object obj = JSONValue.parse(deobfuscatedMetadata);
        if (obj == null) {
            return null;
        }

        if (!(obj instanceof Map)) {
            return null;
        }

        final Map<Object, Object> delegate = (Map<Object, Object>) obj;
        return new InboundHeaders() {

            @Override
            public HeaderType getHeaderType() {
                return HeaderType.MESSAGE;
            }

            @Override
            public String getHeader(String name) {
                if (delegate.containsKey(name)) {
                    return delegate.get(name).toString();
                }
                return null;
            }
        };
    }

}
