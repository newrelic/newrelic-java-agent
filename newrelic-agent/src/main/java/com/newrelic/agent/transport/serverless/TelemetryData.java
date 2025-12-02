package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.CountStats;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.trace.TransactionTrace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class TelemetryData {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<TransactionTrace> transactionTraces = new ArrayList<>();

    private List<SqlTrace> sqlTraces = new ArrayList<>();

    private Integer spanReservoirSize = 0;
    private Integer spanEventsSeen = 0;
    Collection<SpanEvent> spanEvents = new ArrayList<>();

    private Integer errorReservoirSize = 0;
    private Integer errorEventsSeen = 0;
    private Collection<ErrorEvent> errorEvents = new ArrayList<>();
    private List<TracedError> tracedErrors = new ArrayList<>();

    private Integer analyticReservoirSize = 0;
    private Integer analyticEventsSeen = 0;
    private final Collection<AnalyticsEvent> analyticEvents = new ArrayList<>();

    Long metricBeginTimeMillis = 0L;
    Long metricEndTimeMillis = 0L;
    List<MetricData> metricData = new ArrayList<>();

    public TelemetryData() {
    }

    public Map<String, Object> format() {
        try {
            lock.readLock().lock();
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
                addErrors(tracedErrors, data);
            }

            if (!metricData.isEmpty()) {
                addMetrics(metricData, data, metricBeginTimeMillis, metricEndTimeMillis);
            }

            if (!sqlTraces.isEmpty()) {
                addSqlTraces(sqlTraces, data);
            }

            return data;
        } finally {
            lock.readLock().unlock();
        }

    }

    private static <T> void addEvents(Collection<T> events, Map<String, Object> data, String eventKey,
            Integer eventsSeen, Integer reservoirSize) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);

        final Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("events_seen", eventsSeen);
        eventInfo.put("reservoir_size", reservoirSize);
        list.add(1, eventInfo);

        ArrayList<List<Map<String, Object>>> formattedEvents = new ArrayList<>();

        for (Object event : events) {
            Map<String, Object> intrinsicAttributes = new HashMap<>();
            Map<String, Object> userAttributes = new HashMap<>();
            Map<String, Object> agentAttributes = new HashMap<>();

            if (event instanceof AnalyticsEvent) {
                AnalyticsEvent analyticsEvent = (AnalyticsEvent) event;
                userAttributes = analyticsEvent.getUserAttributesCopy();
                intrinsicAttributes.put("type", analyticsEvent.getType());
            }

            if (event instanceof ErrorEvent) {
                ErrorEvent errorEvent = (ErrorEvent) event;
                intrinsicAttributes.putAll(errorEvent.getDistributedTraceIntrinsics());
                agentAttributes.putAll(errorEvent.getAgentAttributes());
            }

            if (event instanceof SpanEvent) {
                SpanEvent spanEvent = (SpanEvent) event;
                intrinsicAttributes.putAll(spanEvent.getIntrinsics());
                agentAttributes.putAll(spanEvent.getAgentAttributes());
            }

            if (event instanceof TransactionEvent) {
                TransactionEvent transactionEvent = (TransactionEvent) event;
                intrinsicAttributes.putAll(transactionEvent.getDistributedTraceIntrinsics());
                intrinsicAttributes.put("name", transactionEvent.getName());
                intrinsicAttributes.put("duration", transactionEvent.getDuration());
                intrinsicAttributes.put("databaseDuration", transactionEvent.getDatabaseDuration());
                intrinsicAttributes.put("timestamp", transactionEvent.getTimestamp());

                agentAttributes.putAll(transactionEvent.getAgentAttributesCopy());
            }

            formattedEvents.add(Arrays.asList(intrinsicAttributes, userAttributes, agentAttributes));
        }
        list.add(2, formattedEvents);
        data.put(eventKey, list);
    }

    private static void addTransactions(List<TransactionTrace> transactionTraces, Map<String, Object> data) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);

        List<List<Object>> formattedTransactions = new ArrayList<>();
        for (TransactionTrace trace : transactionTraces) {
            List<Object> formattedTrace = new ArrayList<>();
            formattedTrace.add(trace.getStartTime());
            formattedTrace.add(trace.getDuration());
            formattedTrace.add(trace.getRootMetricName());
            formattedTrace.add(trace.getRequestUri());
            formattedTrace.add(trace.getTraceDetailsAsList());
            formattedTrace.add(trace.getGuid());
            formattedTrace.add(null);
            formattedTrace.add(false);
            formattedTrace.add(null);
            formattedTrace.add(trace.getSyntheticsResourceId());
        }
        list.add(1, formattedTransactions);
        data.put("transaction_sample_data", list);
    }

    private static void addMetrics(List<MetricData> metrics, Map<String, Object> data, Long metricsBegin, Long metricsEnd) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);
        list.add(1, metricsBegin);
        list.add(2, metricsEnd);

        List<List<Object>> formattedMetrics = new ArrayList<>();

        for (MetricData metricData : metrics) {
            HashMap<String, String> metricStrings = new HashMap<>();
            metricStrings.put("name", metricData.getMetricName().getName());
            metricStrings.put("scope", metricData.getMetricName().getScope());
            List<Number> formattedStats = formatMetricStats(metricData);
            formattedMetrics.add(Arrays.asList(metricStrings, formattedStats));
        }

        list.add(3, formattedMetrics);
        data.put("metric_data", list);
    }

    private static List<Number> formatMetricStats(MetricData metricData) {
        List<Number> formattedStats = new ArrayList<>();
        StatsBase stats = metricData.getStats();
        if (stats instanceof CountStats) {
            CountStats countStats = (CountStats) stats;
            formattedStats.add(countStats.getCallCount());
            formattedStats.add(countStats.getTotal());
            formattedStats.add(countStats.getTotalExclusiveTime());
            formattedStats.add(countStats.getMaxCallTime());
            formattedStats.add(countStats.getMinCallTime());
            formattedStats.add(countStats.getSumOfSquares());
        }
        return formattedStats;
    }

    private static void addSqlTraces(List<SqlTrace> sqlTraces, Map<String, Object> data) {
        List<Object> list = new ArrayList<>();
        List<List<Object>> formattedSqlTraces = new ArrayList<>();
        for (SqlTrace sqlTrace : sqlTraces) {
            List<Object> formattedSqlTrace = new ArrayList<>();
            formattedSqlTrace.add(sqlTrace.getBlameMetricName());
            formattedSqlTrace.add(sqlTrace.getUri());
            formattedSqlTrace.add(sqlTrace.getId());
            formattedSqlTrace.add(sqlTrace.getQuery());
            formattedSqlTrace.add(sqlTrace.getMetricName());
            formattedSqlTrace.add(sqlTrace.getCallCount());
            formattedSqlTrace.add(sqlTrace.getTotal());
            formattedSqlTrace.add(sqlTrace.getMin());
            formattedSqlTrace.add(sqlTrace.getMax());
            // Todo: Reformat params
            formattedSqlTrace.add(sqlTrace.getParameters());
            formattedSqlTraces.add(formattedSqlTrace);
        }
        list.add(0, formattedSqlTraces);
        data.put("sql_trace_data", list);
    }

    private static void addErrors(List<TracedError> tracedErrors, Map<String, Object> data) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);
        List<List<Object>> formattedErrors = new ArrayList<>();
        for (TracedError tracedError : tracedErrors) {
            List<Object> formattedError = new ArrayList<>();
            formattedError.add(tracedError.getTimestampInMillis());
            formattedError.add(tracedError.getPath());
            formattedError.add(tracedError.getMessage());
            formattedError.add(tracedError.getExceptionClass());

            Map<String, Object> errorTraceAttributes = new HashMap<>();
            errorTraceAttributes.put("agentAttributes", tracedError.getAgentAtts());
            errorTraceAttributes.put("intrinsics", tracedError.getIntrinsicAtts());
            errorTraceAttributes.put("request_uri", tracedError.getAgentAtts().get(AttributeNames.REQUEST_URI));
            errorTraceAttributes.put("stack_trace", tracedError.stackTrace());
            errorTraceAttributes.put("userAttributes", tracedError.getAgentAtts());

            formattedError.add(errorTraceAttributes);
            formattedError.add(tracedError.getTransactionGuid());
            formattedErrors.add(formattedError);

        }
        data.put("error_data", Arrays.asList(null, formattedErrors));
    }

    public void updateMetricData(List<MetricData> metricData) {
        try {
            lock.writeLock().lock();
            this.metricData.addAll( metricData);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateMetricEndTimeMillis(Long metricEndTimeMillis) {
        try {
            lock.writeLock().lock();
            if (this.metricEndTimeMillis == 0) {
                this.metricEndTimeMillis = metricEndTimeMillis;
            } else {
                if (metricEndTimeMillis > this.metricEndTimeMillis) {
                    this.metricEndTimeMillis = metricEndTimeMillis;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateMetricBeginTimeMillis(Long metricBeginTimeMillis) {
        try {
            lock.writeLock().lock();
            if (this.metricBeginTimeMillis == 0) {
                this.metricBeginTimeMillis = metricBeginTimeMillis;
            } else {
                if (metricBeginTimeMillis < this.metricBeginTimeMillis) {
                    this.metricBeginTimeMillis = metricBeginTimeMillis;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateTransactionTraces(List<TransactionTrace> transactionTraces) {
        try {
            lock.writeLock().lock();
            this.transactionTraces.addAll(transactionTraces);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateErrorReservoirSize(Integer errorReservoirSize) {
        try {
            lock.writeLock().lock();
            this.errorReservoirSize += errorReservoirSize;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateErrorEventsSeen(Integer errorEventsSeen) {
        try {
            lock.writeLock().lock();
            this.errorEventsSeen += errorEventsSeen;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateErrorEvents(Collection<ErrorEvent> errorEvents) {
        try {
            lock.writeLock().lock();
            this.errorEvents.addAll(errorEvents);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateAnalyticReservoirSize(Integer analyticReservoirSize) {
        try {
            lock.writeLock().lock();
            this.analyticReservoirSize += analyticReservoirSize;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateAnalyticEventsSeen(Integer analyticEventsSeen) {
        try {
            lock.writeLock().lock();
            this.analyticEventsSeen += analyticEventsSeen;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateAnalyticEvents(Collection<? extends AnalyticsEvent> analyticEvents) {
        try {
            lock.writeLock().lock();
            this.analyticEvents.addAll(analyticEvents);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateSpanEvents(Collection<SpanEvent> spanEvents) {
        try {
            lock.writeLock().lock();
            this.spanEvents.addAll(spanEvents);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateSpanEventsSeen(Integer spanEventsSeen) {
        try {
            lock.writeLock().lock();
            this.spanEventsSeen += spanEventsSeen;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateSpanReservoirSize(Integer spanReservoirSize) {
        try {
            lock.writeLock().lock();
            this.spanReservoirSize = spanReservoirSize;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateTracedErrors(List<TracedError> errors) {
        try {
            lock.writeLock().lock();
            this.tracedErrors.addAll(errors);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateSqlTraces(List<SqlTrace> sqlTraces) {
        try {
            lock.writeLock().lock();
            this.sqlTraces.addAll(sqlTraces);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
