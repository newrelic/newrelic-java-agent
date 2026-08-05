/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.utils.audit;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.sdk.common.export.HttpResponse;

import java.text.MessageFormat;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Static helpers for OTLP audit logging.
 */
public final class OtlpAuditLogger {
    private static final String AUDIT_MODE = "audit_mode";
    private static final String OTLP = "OTLP";
    private static final String METRICS = "Metrics";
    private static final Boolean AUDIT_MODE_DEFAULT = false;

    private static final AtomicLong bytesSent = new AtomicLong(0);
    private static final AtomicLong bytesReceived = new AtomicLong(0);

    private OtlpAuditLogger() {
    }

    public static boolean isAuditModeEnabled() {
        return NewRelic.getAgent().getConfig().getValue(AUDIT_MODE, AUDIT_MODE_DEFAULT);
    }

    public static void setBytesSent(int bytes) {
        bytesSent.set(bytes);
    }

    public static void setBytesReceived(int bytes) {
        bytesReceived.set(bytes);
    }

    public static void resetPayloadBytes() {
        bytesSent.set(0);
        bytesReceived.set(0);
    }

    /**
     * Logs audit response payload for OTLP metrics.
     *
     * @param response HttpResponse instance
     */
    public static void logAuditResponse(HttpResponse response) {
        try {
            int statusCode = response.getStatusCode();
            byte[] body = response.getResponseBody();
            if (body == null) {
                body = new byte[0];
            }
            String payload = Base64.getEncoder().encodeToString(body);

            int bytes = body.length;
            setBytesReceived(bytes);
            NewRelic.getAgent()
                    .getLogger()
                    .log(
                            Level.INFO,
                            "Received OTLP/Metrics response: status={0}, bytes={1}, payload: {2}",
                            statusCode,
                            bytes,
                            payload);

            recordDataUsageMetrics();
        } catch (Exception ignored) {
        }
    }

    /**
     * Record metrics tracking amount of bytes sent and received for each OTLP metric payload
     */
    private static void recordDataUsageMetrics() {
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordDataUsageMetricWork(
                        MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES, OTLP),
                        bytesSent.get(), bytesReceived.get()), MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES + " " + OTLP);

        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordDataUsageMetricWork(
                        MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES, OTLP, METRICS),
                        bytesSent.get(), bytesReceived.get()),
                MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES + " " + OTLP);

        // Reset byte counts after recording usage metrics
        resetPayloadBytes();
    }
}
