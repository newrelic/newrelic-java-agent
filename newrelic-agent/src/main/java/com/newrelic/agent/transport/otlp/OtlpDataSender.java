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
import com.newrelic.agent.config.OtlpExportConfig;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
    static final String LOGS_SIGNAL = "v1/logs";
    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";
    static final String API_KEY_HEADER = "api-key";
    static final String NR_DATA_NET_HOST_SUFFIX = ".nr-data.net";
    static final String NEW_RELIC_COM_HOST_SUFFIX = ".newrelic.com";

    // New Relic rejects OTLP payloads that are 10^6 bytes or larger after compression
    static final int MAX_PAYLOAD_SIZE_IN_BYTES = 1_000_000;

    private static final String DESTINATION = "OTLP";

    private final OtlpExportConfig config;
    private final HttpClientWrapper httpClientWrapper;
    private final boolean auditMode;
    private final URL logsUrl;
    private final Map<String, String> logsRequestHeaders;

    public OtlpDataSender(OtlpExportConfig config, String licenseKey, boolean auditMode, HttpClientWrapper httpClientWrapper)
            throws MalformedURLException {
        this.config = config;
        this.httpClientWrapper = httpClientWrapper;
        this.auditMode = auditMode;
        this.logsUrl = new URL(config.getLogsEndpoint());
        this.logsRequestHeaders = buildRequestHeaders(logsUrl, config.getLogsHeaders(), licenseKey);
    }

    private static Map<String, String> buildRequestHeaders(URL url, Map<String, String> configuredHeaders, String licenseKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_PROTOBUF);
        boolean hasApiKey = false;
        for (String name : configuredHeaders.keySet()) {
            hasApiKey |= API_KEY_HEADER.equalsIgnoreCase(name);
        }
        if (!hasApiKey && licenseKey != null) {
            if (isNewRelicHost(url.getHost())) {
                headers.put(API_KEY_HEADER, licenseKey);
            } else {
                Agent.LOG.log(Level.INFO, "Not sending the license key to {0} because it isn''t a New Relic OTLP endpoint."
                        + " Configure otlp_export headers if the endpoint requires authentication.", url);
            }
        }
        headers.putAll(configuredHeaders);
        return Collections.unmodifiableMap(headers);
    }

    static boolean isNewRelicHost(String host) {
        return host != null && (host.toLowerCase(Locale.ROOT).endsWith(NR_DATA_NET_HOST_SUFFIX) || host.toLowerCase(Locale.ROOT).endsWith(NEW_RELIC_COM_HOST_SUFFIX));
    }

    public boolean isLogsEnabled() {
        return config.isLogsEnabled();
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

        ReadResult result = httpClientWrapper.execute(request, new TimingEventHandler(signalPath, statsService));

        int statusCode = result.getStatusCode();
        String httpCodeMetric = MessageFormat.format(MetricNames.SUPPORTABILITY_OTLP_HTTP_CODE, statusCode);
        statsService.doStatsWork(StatsWorks.getIncrementCounterWork(httpCodeMetric, 1), httpCodeMetric);

        if (auditMode) {
            Agent.LOG.info(MessageFormat.format("Sent OTLP {0} request with {1} events ({2} bytes compressed) to {3}, response code {4}",
                    signalPath, eventCount, compressedPayload.length, url, statusCode));
        }

        if (statusCode != HttpResponseCode.OK && statusCode != HttpResponseCode.ACCEPTED) {
            Agent.LOG.log(Level.FINEST, "OTLP {0} response body: {1}", signalPath, result.getResponseBody());
            throw HttpError.create(statusCode, url.getHost(), compressedPayload.length);
        }

        recordDataUsageMetrics(statsService, signalPath, payload.length, result.getResponseBody());
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
