/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MaxPayloadException;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.transport.DataSenderImpl;
import com.newrelic.agent.transport.HttpClientWrapper;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.transport.HttpResponseCode;
import com.newrelic.agent.transport.ReadResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

/**
 * Sends log events to an OTLP/HTTP endpoint using binary protobuf encoding and gzip compression.
 * <p>
 * Unlike {@link DataSenderImpl}, this sender doesn't use the New Relic collector protocol: there is no agent run id, and
 * the license key is sent in the {@code api-key} header rather than the query string. The license key is only sent to
 * New Relic endpoints, so that it isn't disclosed when data is exported to a third party OTLP endpoint.
 */
public class OtlpDataSender {

    // Signal names used in supportability metrics and log messages
    static final String HTTPS = "https://";
    static final String LOGS_PATH = "/v1/logs";
    static final String LOGS_SIGNAL = "Logs";
    // The name that selects OTLP logs in audit_mode.endpoints, alongside the collector method names
    public static final String AUDIT_ENDPOINT_LOGS = "otlp_logs";
    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";
    static final String API_KEY_HEADER = "api-key";
    static final String NR_DATA_NET_HOST_SUFFIX = ".nr-data.net";
    static final String NEW_RELIC_COM_HOST_SUFFIX = ".newrelic.com";

    // New Relic rejects OTLP payloads that are 10^6 bytes or larger after compression
    static final int MAX_PAYLOAD_SIZE_IN_BYTES = 1_000_000;

    private static final String DESTINATION = "OTLP";

    private final HttpClientWrapper httpClientWrapper;
    private final BooleanSupplier auditMode;
    // The last audit mode that was logged, so that only changes are logged. Null until the first send.
    private volatile Boolean loggedAuditMode;
    private final URL logsUrl;
    private final Map<String, String> logsRequestHeaders;

    public OtlpDataSender(String host, String licenseKey, boolean auditMode, HttpClientWrapper httpClientWrapper)
            throws MalformedURLException {
        this(host, licenseKey, () -> auditMode, httpClientWrapper);
    }

    /**
     * @param auditMode supplies whether audit mode is currently enabled. It is checked on every send, so audit_mode can be changed at runtime.
     */
    public OtlpDataSender(String host, String licenseKey, BooleanSupplier auditMode, HttpClientWrapper httpClientWrapper)
            throws MalformedURLException {
        this.httpClientWrapper = httpClientWrapper;
        this.auditMode = auditMode;
        this.logsUrl = new URL(HTTPS + host + LOGS_PATH);
        this.logsRequestHeaders = buildRequestHeaders(logsUrl, licenseKey);
    }

    private static Map<String, String> buildRequestHeaders(URL url, String licenseKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_PROTOBUF);
        if (licenseKey != null) {
            if (isNewRelicHost(url.getHost())) {
                headers.put(API_KEY_HEADER, licenseKey);
            } else {
                Agent.LOG.log(Level.INFO, "Not sending the license key to {0} because it isn't a New Relic OTLP endpoint.", url);
            }
        }
        return Collections.unmodifiableMap(headers);
    }

    static boolean isNewRelicHost(String host) {
        return host != null &&
                (host.toLowerCase(Locale.ROOT).endsWith(NR_DATA_NET_HOST_SUFFIX) || host.toLowerCase(Locale.ROOT).endsWith(NEW_RELIC_COM_HOST_SUFFIX));
    }

    public void sendLogEvents(Map<String, ?> resourceAttributes, Collection<? extends LogEvent> events) throws Exception {
        if (events.isEmpty()) {
            return;
        }
        send(LOGS_SIGNAL, logsUrl, logsRequestHeaders, OtlpLogEncoder.encode(resourceAttributes, events), events.size());
    }

    public void shutdown() {
        httpClientWrapper.shutdown();
    }

    private void send(String signalPath, URL url, Map<String, String> requestHeaders, byte[] payload, int eventCount) throws Exception {
        StatsService statsService = ServiceFactory.getStatsService();
        byte[] compressedPayload = gzip(payload);

        if (compressedPayload.length >= MAX_PAYLOAD_SIZE_IN_BYTES) {
            String metricName = MessageFormat.format(MetricNames.SUPPORTABILITY_OTLP_PAYLOAD_SIZE_EXCEEDS_MAX, signalPath);
            statsService.doStatsWork(StatsWorks.getIncrementCounterWork(metricName, 1), metricName);
            String message = MessageFormat.format("OTLP payload of {0} bytes for {1} exceeded the maximum size of {2} bytes and was dropped ({3} events)."
                            + " Reduce application_logging.forwarding.max_samples_stored to send smaller payloads.",
                    compressedPayload.length, signalPath,
                    MAX_PAYLOAD_SIZE_IN_BYTES, eventCount);
            Agent.LOG.log(Level.WARNING, message);
            throw new MaxPayloadException(message);
        }

        HttpClientWrapper.Request request = new HttpClientWrapper.Request()
                .setURL(url)
                .setVerb(HttpClientWrapper.Verb.POST)
                .setEncoding(DataSenderImpl.GZIP_ENCODING)
                .setData(compressedPayload)
                .setRequestMetadata(requestHeaders);

        boolean audit = isAuditMode();
        // The license key is sent in a header rather than the URL, so the URL needs no obfuscation
        if (audit) {
            Agent.LOG.info(auditRequestMessage(signalPath, url, payload));
        }

        ReadResult result = httpClientWrapper.execute(request, new TimingEventHandler(signalPath, statsService));

        int statusCode = result.getStatusCode();
        String httpCodeMetric = MessageFormat.format(MetricNames.SUPPORTABILITY_OTLP_HTTP_CODE, statusCode);
        statsService.doStatsWork(StatsWorks.getIncrementCounterWork(httpCodeMetric, 1), httpCodeMetric);

        if (audit) {
            Agent.LOG.info(auditResponseMessage(signalPath, statusCode, result.getResponseBody()));
        }

        if (statusCode != HttpResponseCode.OK && statusCode != HttpResponseCode.ACCEPTED) {
            Agent.LOG.log(Level.FINEST, "OTLP {0} response body: {1}", signalPath, result.getResponseBody());
            throw HttpError.create(statusCode, url.getHost(), compressedPayload.length);
        }

        recordDataUsageMetrics(statsService, signalPath, payload.length, result.getResponseBody());
    }

    /**
     * Whether an endpoint should be audited, using the same rules as DataSenderImpl: audit_mode must be on, and if
     * audit_mode.endpoints is set then the endpoint must be one of them.
     */
    public static boolean shouldAudit(boolean auditModeEnabled, Set<String> auditModeEndpoints, String endpoint) {
        if (!auditModeEnabled) {
            return false;
        }
        return auditModeEndpoints == null || auditModeEndpoints.isEmpty() || auditModeEndpoints.contains(endpoint);
    }

    private boolean isAuditMode() {
        boolean current;
        try {
            current = auditMode.getAsBoolean();
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "Unable to determine whether audit_mode is enabled for OTLP");
            return false;
        }
        logAuditModeChange(current);
        return current;
    }

    /**
     * Logs the audit mode the first time it is seen and whenever it changes, as DataSenderImpl does.
     *
     * @return true if the audit mode was logged
     */
    boolean logAuditModeChange(boolean current) {
        Boolean previous = loggedAuditMode;
        if (previous != null && previous == current) {
            return false;
        }
        loggedAuditMode = current;
        Agent.LOG.info(MessageFormat.format("Setting audit_mode to {0}", current));
        return true;
    }

    /**
     * The audit entry for a request: the destination, and the uncompressed protobuf body base64 encoded with its decoded byte count.
     */
    static String auditRequestMessage(String signalPath, URL url, byte[] payload) {
        return MessageFormat.format("Sent OTLP/{0} to: {1}, bytes: {2}, payload: {3}",
                signalPath, url, String.valueOf(payload.length), Base64.getEncoder().encodeToString(payload));
    }

    /**
     * The audit entry for a response: the status code, and the response body base64 encoded with its decoded byte count.
     */
    static String auditResponseMessage(String signalPath, int statusCode, String responseBody) {
        byte[] body = responseBody == null ? new byte[0] : responseBody.getBytes(StandardCharsets.UTF_8);
        return MessageFormat.format("Received OTLP/{0} response: status={1}, bytes={2}, payload: {3}",
                signalPath, String.valueOf(statusCode), String.valueOf(body.length), Base64.getEncoder().encodeToString(body));
    }

    // Matches DataSenderImpl, which records the uncompressed size of each payload
    private void recordDataUsageMetrics(StatsService statsService, String signalPath, int payloadBytesSent, String responseBody) {
        int payloadBytesReceived = responseBody == null ? 0 : responseBody.length();
        String destinationMetric = MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES, DESTINATION);
        statsService.doStatsWork(StatsWorks.getRecordDataUsageMetricWork(destinationMetric, payloadBytesSent, payloadBytesReceived),
                destinationMetric);
        String endpointMetric = MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES, DESTINATION,
                signalPath);
        statsService.doStatsWork(StatsWorks.getRecordDataUsageMetricWork(endpointMetric, payloadBytesSent, payloadBytesReceived),
                endpointMetric);
    }

    private static byte[] gzip(byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(payload.length / 4, 64));
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(outputStream)) {
            gzipStream.write(payload);
        }
        return outputStream.toByteArray();
    }

    private static class TimingEventHandler implements HttpClientWrapper.ExecuteEventHandler {
        private final String signalPath;
        private final StatsService statsService;
        private long requestStartMillis;

        TimingEventHandler(String signalPath, StatsService statsService) {
            this.signalPath = signalPath;
            this.statsService = statsService;
        }

        @Override
        public void requestStarted() {
            requestStartMillis = System.currentTimeMillis();
        }

        @Override
        public void requestEnded() {
            String metricName = MessageFormat.format(MetricNames.SUPPORTABILITY_OTLP_ENDPOINT_DURATION, signalPath);
            statsService.doStatsWork(StatsWorks.getRecordResponseTimeWork(metricName, System.currentTimeMillis() - requestStartMillis), metricName);
        }
    }
}
