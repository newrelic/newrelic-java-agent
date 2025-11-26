package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TelemetryData {
    private List<TransactionTrace> transactionTraces = new ArrayList<>();

    private List<SqlTrace> sqlTraces = new ArrayList<>();

    private Integer spanReservoirSize;
    private Integer spanEventsSeen;
    Collection<SpanEvent> spanEvents = new ArrayList<>();

    private Integer errorReservoirSize;
    private Integer errorEventsSeen;
    private Collection<ErrorEvent> errorEvents = new ArrayList<>();
    private List<TracedError> tracedErrors = new ArrayList<>();

    private Integer analyticReservoirSize = 0;
    private Integer analyticEventsSeen = 0;
    private final Collection<AnalyticsEvent> analyticEvents = new ArrayList<>();

    Long metricBeginTimeMillis;
    Long metricEndTimeMillis;
    List<MetricData> metricData = new ArrayList<>();

    public TelemetryData() {
    }

    public Map<String, Object> format() {
        Map<String, Object> data = new HashMap<>();

        if (!spanEvents.isEmpty()) {
            addEvents(spanEvents, data, "span_event_data", spanReservoirSize, spanEventsSeen);
        }
        if (!transactionTraces.isEmpty()) {
            addTransactions(transactionTraces, data);
        }

        if (!analyticEvents.isEmpty()) {
            addEvents(analyticEvents, data, "analytic_event_data", analyticReservoirSize, analyticEventsSeen);
        }

        if (!errorEvents.isEmpty()) {
            addEvents(errorEvents, data, "error_event_data", errorReservoirSize, errorEventsSeen);
        }
        if (!tracedErrors.isEmpty()) {
            data.put("error_data", Arrays.asList(null, tracedErrors));
        }

        if (!metricData.isEmpty()) {
            addMetrics(metricData, data, metricBeginTimeMillis, metricEndTimeMillis);
        }

        if (!sqlTraces.isEmpty()) {
            addSqlTraces(sqlTraces, data);
        }

        return data;
    }

    private static <T> void addEvents(Collection<T> events, Map<String, Object> data, String eventKey,
            Integer eventsSeen, Integer reservoirSize) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);

        final Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("events_seen", eventsSeen);
        eventInfo.put("reservoir_size", reservoirSize);
        list.add(1, eventInfo);
        list.add(2, events);
        data.put(eventKey, list);
    }

    private static void addTransactions(List<TransactionTrace> transactionTraces, Map<String, Object> data) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);

        list.add(1, transactionTraces);
        data.put("transaction_sample_data", list);
    }

    private static void addMetrics(List<MetricData> metrics, Map<String, Object> data, Long metricsBegin, Long metricsEnd) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);
        list.add(1, metricsBegin);
        list.add(2, metricsEnd);
        list.add(3, metrics);
        data.put("metric_data", list);
    }

    private static void addSqlTraces(List<SqlTrace> sqlTraces, Map<String, Object> data) {
        List<Object> list = new ArrayList<>();
        list.add(0, sqlTraces);
        data.put("sql_trace_data", list);
    }

    public void setMetricData(List<MetricData> metricData) {
        this.metricData = metricData;
    }

    public void setMetricEndTimeMillis(Long metricEndTimeMillis) {
        this.metricEndTimeMillis = metricEndTimeMillis;
    }

    public void setMetricBeginTimeMillis(Long metricBeginTimeMillis) {
        this.metricBeginTimeMillis = metricBeginTimeMillis;
    }

    public void setTransactionTraces(List<TransactionTrace> transactionTraces) {
        this.transactionTraces = transactionTraces;
    }

    public void setErrorReservoirSize(Integer errorReservoirSize) {
        this.errorReservoirSize = errorReservoirSize;
    }

    public void setErrorEventsSeen(Integer errorEventsSeen) {
        this.errorEventsSeen = errorEventsSeen;
    }

    public void setErrorEvents(Collection<ErrorEvent> errorEvents) {
        this.errorEvents = errorEvents;
    }

    public void updateAnalyticReservoirSize(Integer analyticReservoirSize) {
        this.analyticReservoirSize += analyticReservoirSize;
    }

    public void updateAnalyticEventsSeen(Integer analyticEventsSeen) {
        this.analyticEventsSeen += analyticEventsSeen;
    }

    public void updateAnalyticEvents(Collection<? extends AnalyticsEvent> analyticEvents) {
        this.analyticEvents.addAll(analyticEvents);
    }

    public void setSpanEvents(Collection<SpanEvent> spanEvents) {
        this.spanEvents = spanEvents;
    }

    public void setSpanEventsSeen(Integer spanEventsSeen) {
        this.spanEventsSeen = spanEventsSeen;
    }

    public void setSpanReservoirSize(Integer spanReservoirSize) {
        this.spanReservoirSize = spanReservoirSize;
    }

    public void setTracedErrors(List<TracedError> errors) {
        this.tracedErrors = errors;
    }

    public void setSqlTraces(List<SqlTrace> sqlTraces) {
        this.sqlTraces = sqlTraces;
    }
}
