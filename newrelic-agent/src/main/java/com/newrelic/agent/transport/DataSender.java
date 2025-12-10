/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;
import org.json.simple.JSONStreamAware;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSender {

    Map<String, Object> connect(Map<String, Object> startupOptions) throws Exception;

    List<List<?>> getAgentCommands() throws Exception;

    void sendCommandResults(Map<Long, Object> commandResults) throws Exception;

    void sendErrorData(List<TracedError> errors) throws Exception;

    void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> errorEvents) throws Exception;

    /**
     * Send non-aggregated events for analytics
     */
    <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<T> events) throws Exception;

    /**
     * Send non-aggregated custom events for analytics
     */
    void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception;

    /**
     * Send non-aggregated Log events
     */
    void sendLogEvents(Collection<? extends LogEvent> events) throws Exception;

    /**
     * Send non-aggregated span events
     */
    void sendSpanEvents(int reservoirSize, int eventsSeen, Collection<SpanEvent> events) throws Exception;

    /**
     * Send metric data to New Relic.
     *
     * @param beginTimeMillis the last time metric data was sent to New Relic
     * @param endTimeMillis the time now
     * @param metricData the metric data to send
     * @throws Exception if there is a problem sending the metric data
     */
    void sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception;

    /**
     * Send thread profiles to New Relic.
     *
     * @param profiles the profiles to send
     * @return a list of profile IDs for the profiles
     * @throws Exception if there is a problem sending the profiles
     */
    List<Long> sendProfileData(List<ProfileData> profiles) throws Exception;

    void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception;

    void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception;

    void sendModules(List<? extends JSONStreamAware> jarDataToSend) throws Exception;

    void shutdown(long timeMillis) throws Exception;

    /**
     * Saves and flushes any buffered telemetry. Used for serverless harvests and is a No-Op in all other scenarios.
     */
    void commitAndFlush() throws Exception;

}
