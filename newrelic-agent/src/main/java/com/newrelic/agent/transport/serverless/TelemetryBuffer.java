package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread safe buffer to store telemetry to be outputted in serverless mode.
 * Whenever telemetry is fed into the DataSender during a serverless harvest, it will be stored in this buffer until the harvest finishes.
 * To finish a serverless harvest, the buffered data is formatted into JSON payloads to be sent to the ServerlessWriter.
 * Afterward, the buffer is cleared for the next harvest.
 */
class TelemetryBuffer {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<TransactionTrace> transactionTraces = new ArrayList<>();

    private final List<SqlTrace> sqlTraces = new ArrayList<>();

    private Integer spanReservoirSize = 0;
    private Integer spanEventsSeen = 0;
    private final Collection<SpanEvent> spanEvents = new ArrayList<>();

    private Integer errorReservoirSize = 0;
    private Integer errorEventsSeen = 0;
    private final Collection<ErrorEvent> errorEvents = new ArrayList<>();
    private final List<TracedError> tracedErrors = new ArrayList<>();

    private Integer analyticReservoirSize = 0;
    private Integer analyticEventsSeen = 0;
    private final Collection<AnalyticsEvent> analyticEvents = new ArrayList<>();

    private Integer customEventsReservoirSize = 0;
    private Integer customEventsSeen = 0;

    private Integer logEventsReservoirSize = 0;
    private Integer logEventsSeen = 0;

    private Long metricBeginTimeMillis = 0L;
    private Long metricEndTimeMillis = 0L;
    private final List<MetricData> metricData = new ArrayList<>();

    public TelemetryBuffer() {
    }

    public JSONObject toJsonObject() {
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

        JSONArray eventParameters = new JSONArray();

        for (Object event : events) {
            if (event instanceof TransactionEvent || event instanceof ErrorEvent) {
                eventParameters.add(event);
                continue;
            } else if (event instanceof SpanEvent) {
                /* (Copy/paste of the same comment in com.newrelic.agent.transport.DataSenderImpl)
                 *
                 * When creating the span_event_data json payload structure we need to pull
                 * all links and events off each span and treat them as if they were spans
                 * themselves. This is because links and events are not treated as first class event
                 * types with their own reservoir, and they also can't be stored alongside spans in
                 * the span reservoir. Yet they are treated as if they were spans in the json payload.
                 * This was a hacky way to get the backend to accept new SpanLink/SpanEvent event
                 * types without setting up a dedicated collector endpoint for them. The backend
                 * will synthesize SpanLink/SpanEvent events based on the span_event_data payload.
                 */
                eventParameters.add(event);
                eventParameters.addAll(((SpanEvent) event).getLinkOnSpanEvents());
                eventParameters.addAll(((SpanEvent)event).getEventOnSpanEvents());
                continue;
            }

            Map<String, Object> intrinsicAttributes = new HashMap<>();
            Map<String, Object> userAttributes = new HashMap<>();
            Map<String, Object> agentAttributes = new HashMap<>();

            if (event instanceof AnalyticsEvent) {
                AnalyticsEvent analyticsEvent = (AnalyticsEvent) event;
                userAttributes = analyticsEvent.getUserAttributesCopy();
                intrinsicAttributes.put("type", analyticsEvent.getType());
            }
            JSONArray eventData = new JSONArray();
            eventData.add(intrinsicAttributes);
            eventData.add(userAttributes);
            eventData.add(agentAttributes);

            eventParameters.add(eventData);
        }
        list.add(eventParameters);
        data.put(eventKey, list);
    }

    private static void addTransactions(List<TransactionTrace> transactionTraces, JSONObject data) {
        JSONArray list = new JSONArray();
        list.add(0, null);
        list.add(1, transactionTraces);
        data.put("transaction_sample_data", list);
    }

    private static void addMetrics(List<MetricData> metrics, JSONObject data, Long metricsBegin, Long metricsEnd) {
        JSONArray list = new JSONArray();
        list.add(null);
        list.add(metricsBegin);
        list.add(metricsEnd);

        list.add(metrics);
        data.put("metric_data", list);
    }

    private static void addSqlTraces(List<SqlTrace> sqlTraces, JSONObject data) {
        JSONArray list = new JSONArray();
        list.add(sqlTraces);
        data.put("sql_trace_data", list);
    }

    private static void addErrors(List<TracedError> tracedErrors, Map<String, Object> data) {
        JSONArray list = new JSONArray();
        list.add(0, null);
        list.add(tracedErrors);
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

    public void clear() {
        try{
            lock.writeLock().lock();
            this.transactionTraces.clear();

            this.sqlTraces.clear();

            this.spanReservoirSize = 0;
            this.spanEventsSeen = 0;
            this.spanEvents.clear();

            this.errorReservoirSize = 0;
            this.errorEventsSeen = 0;
            this.errorEvents.clear();
            this.tracedErrors.clear();

            this.analyticReservoirSize = 0;
            this.analyticEventsSeen = 0;
            this.analyticEvents.clear();

            this.customEventsReservoirSize = 0;
            this.customEventsSeen = 0;

            this.logEventsReservoirSize = 0;
            this.logEventsSeen = 0;

            this.metricBeginTimeMillis = 0L;
            this.metricEndTimeMillis = 0L;
            this.metricData.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
