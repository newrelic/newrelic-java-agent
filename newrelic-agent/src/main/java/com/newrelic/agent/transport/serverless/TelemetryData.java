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
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTrace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

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

    private Integer customEventsReservoirSize = 0;
    private Integer customEventsSeen = 0;

    private Integer logEventsReservoirSize = 0;
    private Integer logEventsSeen = 0;

    Long metricBeginTimeMillis = 0L;
    Long metricEndTimeMillis = 0L;
    List<MetricData> metricData = new ArrayList<>();

    public TelemetryData() {
    }

    public JSONObject format() {
        try {
            lock.readLock().lock();

            JSONObject data = new JSONObject();


            if (!spanEvents.isEmpty()) {
                addEvents(spanEvents, data, "span_event_data", spanEventsSeen, spanReservoirSize);
            }
            if (!transactionTraces.isEmpty()) {
                addTransactions(transactionTraces, data);
            }

            if (!analyticEvents.isEmpty()) {
                int reservoirSize = analyticReservoirSize + customEventsReservoirSize + logEventsReservoirSize;
                int eventsSeen = analyticEventsSeen + customEventsSeen + logEventsSeen;
                addEvents(analyticEvents, data, "analytic_event_data", eventsSeen, reservoirSize);
            }

            if (!errorEvents.isEmpty()) {
                addEvents(errorEvents, data, "error_event_data", errorEventsSeen, errorReservoirSize);
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

    private static <T> void addEvents(Collection<T> events, JSONObject data, String eventKey,
            Integer eventsSeen, Integer reservoirSize) {
        JSONArray list = new JSONArray();
        list.add(null);

        final JSONObject eventInfo = new JSONObject();
        eventInfo.put("events_seen", eventsSeen);
        eventInfo.put("reservoir_size", reservoirSize);
        list.add(eventInfo);

        JSONArray formattedEvents = new JSONArray();

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

            JSONArray eventData = new JSONArray();
            eventData.add(intrinsicAttributes);
            eventData.add(userAttributes);
            eventData.add(agentAttributes);

            formattedEvents.add(eventData);
        }
        list.add(formattedEvents);
        data.put(eventKey, list);
    }

    private static void addTransactions(List<TransactionTrace> transactionTraces, JSONObject data) {
        JSONArray list = new JSONArray();
        list.add(0, null);

        JSONArray formattedTransactions = new JSONArray();
        for (TransactionTrace trace : transactionTraces) {
            JSONArray traceData = new JSONArray();
            traceData.add(trace.getStartTime());
            traceData.add(trace.getDuration());
            traceData.add(trace.getRootMetricName());
            traceData.add(trace.getRequestUri());
            JSONArray traceDetailsJson = new JSONArray();
            for (Object item : trace.getTraceDetailsAsList()) {
                if (item instanceof TransactionSegment) {
                    TransactionSegment txnSeg = (TransactionSegment) item;
                    JSONArray segmentJson = formatTransactionSegment(txnSeg);
                    traceDetailsJson.add(segmentJson);
                } else {
                    traceDetailsJson.add(item);
                }
            }
            traceData.add(traceDetailsJson);
            traceData.add(trace.getGuid());
            traceData.add(null);
            traceData.add(false);
            traceData.add(null);
            traceData.add(trace.getSyntheticsResourceId());
            formattedTransactions.add(traceData);
        }
        list.add(1, formattedTransactions);
        data.put("transaction_sample_data", list);
    }

    private static JSONArray formatTransactionSegment(TransactionSegment txnSeg) {
        JSONArray segmentJSON = new JSONArray();
        segmentJSON.add(txnSeg.getStartTime());
        segmentJSON.add(txnSeg.getEndTime());
        segmentJSON.add(txnSeg.getMetricName());
        segmentJSON.add(txnSeg.getFilteredAttributes());
        JSONArray children = new JSONArray();
        if (txnSeg.getChildren() != null) {
            for (TransactionSegment item : txnSeg.getChildren()) {
                JSONArray childSegmentJson = formatTransactionSegment(item);
                children.add(childSegmentJson);
            }
        }
        segmentJSON.add(children);
        segmentJSON.add(txnSeg.getClassName());
        segmentJSON.add(txnSeg.getMethodName());
        return segmentJSON;
    }

    private static void addMetrics(List<MetricData> metrics, JSONObject data, Long metricsBegin, Long metricsEnd) {
        JSONArray list = new JSONArray();
        list.add(null);
        list.add(metricsBegin);
        list.add(metricsEnd);

        JSONArray formattedMetrics = new JSONArray();

        for (MetricData metricData : metrics) {
            JSONObject metricStrings = new JSONObject();
            metricStrings.put("name", metricData.getMetricName().getName());
            metricStrings.put("scope", metricData.getMetricName().getScope());

            JSONArray statsData = formatMetricStats(metricData);

            JSONArray metricJson = new JSONArray();
            metricJson.add(metricStrings);
            metricJson.add(statsData);

            formattedMetrics.add(metricJson);
        }

        list.add(formattedMetrics);
        data.put("metric_data", list);
    }

    private static JSONArray formatMetricStats(MetricData metricData) {
        JSONArray formattedStats = new JSONArray();
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

    private static void addSqlTraces(List<SqlTrace> sqlTraces, JSONObject data) {
        JSONArray list = new JSONArray();
        JSONArray formattedSqlTraces = new JSONArray();
        for (SqlTrace sqlTrace : sqlTraces) {
            JSONArray sqlTraceData = new JSONArray();
            sqlTraceData.add(sqlTrace.getBlameMetricName());
            sqlTraceData.add(sqlTrace.getUri());
            sqlTraceData.add(sqlTrace.getId());
            sqlTraceData.add(sqlTrace.getQuery());
            sqlTraceData.add(sqlTrace.getMetricName());
            sqlTraceData.add(sqlTrace.getCallCount());
            sqlTraceData.add(sqlTrace.getTotal());
            sqlTraceData.add(sqlTrace.getMin());
            sqlTraceData.add(sqlTrace.getMax());
            sqlTraceData.add(compressAndEncodeSqlParams(sqlTrace.getParameters()));
            formattedSqlTraces.add(sqlTraceData);
        }

        list.add(formattedSqlTraces);
        data.put("sql_trace_data", list);
    }

    private static Object compressAndEncodeSqlParams(Map<String, Object> params) {
        String paramsJson = JSONObject.toJSONString(params).replace("\\/","/");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(output);
            gzip.write(paramsJson.getBytes(UTF_8));
            gzip.flush();
            gzip.close();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ignored) {
        }
        return params;
    }

    private static void addErrors(List<TracedError> tracedErrors, Map<String, Object> data) {
        JSONArray list = new JSONArray();
        list.add(0, null);
        JSONArray formattedErrors = new JSONArray();
        for (TracedError tracedError : tracedErrors) {
            JSONArray errorData = new JSONArray();
            errorData.add(tracedError.getTimestampInMillis());
            errorData.add(tracedError.getPath());
            errorData.add(tracedError.getMessage());
            errorData.add(tracedError.getExceptionClass());

            Map<String, Object> errorTraceAttributes = new HashMap<>();
            errorTraceAttributes.put("agentAttributes", tracedError.getAgentAtts());
            errorTraceAttributes.put("intrinsics", tracedError.getIntrinsicAtts());
            errorTraceAttributes.put("request_uri", tracedError.getAgentAtts().get(AttributeNames.REQUEST_URI));
            errorTraceAttributes.put("stack_trace", tracedError.stackTrace());
            errorTraceAttributes.put("userAttributes", Collections.emptyMap());

            errorData.add(errorTraceAttributes);
            errorData.add(tracedError.getTransactionGuid());
            formattedErrors.add(errorData);

        }
        list.add(formattedErrors);
        data.put("error_data", list);
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

    public void updateCustomEventsReservoirSize(int reservoirSize) {
        try {
            lock.writeLock().lock();
            this.customEventsReservoirSize += reservoirSize;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateCustomEventsSeen(int eventsSeen) {
        try {
            lock.writeLock().lock();
            this.customEventsSeen += eventsSeen;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateLogEventsReservoir(int size) {
        try {
            lock.writeLock().lock();
            this.logEventsReservoirSize += size;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateLogEventsSeen(int seen) {
        try {
            lock.writeLock().lock();
            this.logEventsSeen += seen;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
