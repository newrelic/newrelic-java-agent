/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.DataSenderConfig;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.profile.ProfileData;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.transport.DataSender;
import com.newrelic.agent.transport.DataSenderListener;
import org.json.simple.JSONStreamAware;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class MockDataSender implements DataSender {

    public static final String COLLECT_TRACES_KEY = "collect_traces";
    private static final String COLLECT_ERRORS_KEY = "collect_errors";
    private static final String APDEX_T_KEY = "apdex_t";
    private static final String DATA_REPORT_PERIOD_KEY = "data_report_period";

    private List<TransactionTrace> traces;
    private List<LogEvent> logEvents;
    Map<String, Object> startupOptions;
    private Exception exception;
    private boolean isConnected;
    private CountDownLatch latch;

    public MockDataSender(DataSenderConfig config) {
        this(config, null);
    }

    public MockDataSender(DataSenderConfig config, DataSenderListener dataSenderListener) {
    }

    @Override
    public Map<String, Object> connect(Map<String, Object> startupOptions) throws Exception {
        this.startupOptions = startupOptions;
        isConnected = true;
        if (latch != null) {
            latch.countDown();
        }
        Map<String, Object> data = new HashMap<>();
        data.put(COLLECT_ERRORS_KEY, true);
        data.put(COLLECT_TRACES_KEY, true);
        data.put(DATA_REPORT_PERIOD_KEY, 60L);
        data.put(APDEX_T_KEY, 1.0D);
        return data;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public List<List<?>> getAgentCommands() throws Exception {
        return null;
    }

    @Override
    public void sendCommandResults(Map<Long, Object> commandResults) throws Exception {
    }

    @Override
    public void sendErrorData(List<TracedError> errors) throws Exception {
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData) throws Exception {
    }

    @Override
    public List<Long> sendProfileData(List<ProfileData> profiles) throws Exception {
        return null;
    }

    @Override
    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        if (exception != null) {
            throw exception;
        }
        this.traces = traces;
    }

    @Override
    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
    }

    public List<TransactionTrace> getTraces() {
        return traces;
    }

    public List<LogEvent> getLogEvents() {
        return logEvents;
    }

    public Map<String, Object> getStartupOtions() {
        return startupOptions;
    }

    @Override
    public void shutdown(long timeMillis) throws Exception {
    }

    @Override
    public void sendModules(List<? extends JSONStreamAware> jarData) throws Exception {
    }

    @Override
    public void sendErrorEvents(int reservoirSize, int eventsSeen, Collection<ErrorEvent> errorEvents) throws Exception {
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public <T extends AnalyticsEvent & JSONStreamAware> void sendAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<T> events) throws Exception {
    }

    @Override
    public void sendSpanEvents(int reservoirSize, int eventsSeen, final Collection<SpanEvent> events) throws Exception {
    }

    @Override
    public void sendCustomAnalyticsEvents(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> events) throws Exception {
    }

    @Override
    public void sendDimensionalMetricData(int reservoirSize, int eventsSeen, Collection<? extends CustomInsightsEvent> metricData) {
    }

    @Override
    public void sendLogEvents(Collection<? extends LogEvent> events) throws Exception {
        if (exception != null) {
            throw exception;
        }
        logEvents = new ArrayList<>(events);
    }
}
