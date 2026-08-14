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

import java.text.MessageFormat;

/**
 * Static helpers for OTLP audit logging.
 */
public final class OtlpAuditLogger {
    private static final String AUDIT_MODE = "audit_mode";
    private static final String OTLP = "OTLP";
    private static final String METRICS = "Metrics";
    private static final Boolean AUDIT_MODE_DEFAULT = false;

    private OtlpAuditLogger() {
    }

    public static boolean isAuditModeEnabled() {
        return NewRelic.getAgent().getConfig().getValue(AUDIT_MODE, AUDIT_MODE_DEFAULT);
    }

    /**
     * Records Supportability/DataUsage metrics for an OTLP export cycle.
     */
    public static void recordDataUsageMetrics(long bytesSent, long bytesReceived) {
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordDataUsageMetricWork(
                        MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES, OTLP),
                        bytesSent, bytesReceived),
                MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES + " " + OTLP);
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordDataUsageMetricWork(
                        MessageFormat.format(MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES, OTLP, METRICS),
                        bytesSent, bytesReceived),
                MetricNames.SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES + " " + OTLP);
    }
}
