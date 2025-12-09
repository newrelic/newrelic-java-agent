/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.serverless;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.model.AnalyticsEvent;
import com.newrelic.agent.model.ApdexPerfZone;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.CountedDuration;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.model.ErrorEvent;
import com.newrelic.agent.model.LogEvent;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.model.SyntheticsIds;
import com.newrelic.agent.model.SyntheticsInfo;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.analytics.TransactionEventBuilder;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTrace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class TelemetryBufferTest {
    @Test
    public void testTracedErrors() {
        TelemetryBuffer data = new TelemetryBuffer();
        TracedError tracedError = Mockito.mock(TracedError.class);
        Mockito.when(tracedError.getTimestampInMillis()).thenReturn(10L);
        Mockito.when(tracedError.getMessage()).thenReturn("message");
        Mockito.when(tracedError.getPath()).thenReturn("/path");
        Mockito.when(tracedError.getExceptionClass()).thenReturn("exceptionClass");
        HashMap<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put(AttributeNames.REQUEST_URI, "localhost:8080");
        Mockito.when(tracedError.getAgentAtts()).thenReturn(agentAttributes);
        Map intrinsicAttrs = new HashMap<>();
        intrinsicAttrs.put("attr1", "val1");
        Mockito.when(tracedError.getIntrinsicAtts()).thenReturn(intrinsicAttrs);
        Mockito.when(tracedError.stackTrace()).thenReturn(Arrays.asList("stackTrace1", "stackTrace2"));
        Mockito.when(tracedError.getTransactionGuid()).thenReturn("transactionGuid");

        data.updateTracedErrors(Arrays.asList(tracedError));

        JSONObject formatted = data.formatJson();

        Assert.assertTrue(formatted.containsKey("error_data"));

        JSONArray list = (JSONArray) formatted.get("error_data");
        Assert.assertEquals(2, list.size());
        Assert.assertNull(list.get(0));

        JSONArray errors = (JSONArray) list.get(1);
        Assert.assertEquals(1, errors.size());

        JSONArray error = (JSONArray) errors.get(0);
        Assert.assertEquals(6, error.size());

        Assert.assertEquals(10L, error.get(0));
        Assert.assertEquals("/path", error.get(1));
        Assert.assertEquals("message", error.get(2));
        Assert.assertEquals("exceptionClass", error.get(3));

        Assert.assertTrue(error.get(4) instanceof Map);
        Map<String, Object> attributes = (Map<String, Object>) error.get(4);

        Assert.assertEquals("localhost:8080", ((Map<?, ?>) attributes.get("agentAttributes")).get(AttributeNames.REQUEST_URI));
        Assert.assertEquals("val1", ((Map<?, ?>) attributes.get("intrinsics")).get("attr1"));
        Assert.assertEquals("localhost:8080", attributes.get("request_uri"));

        Assert.assertTrue(((Collection<String>) attributes.get("stack_trace")).contains("stackTrace1"));
        Assert.assertTrue(((Collection<String>) attributes.get("stack_trace")).contains("stackTrace2"));

        Assert.assertEquals(0, ((Map<?, ?>)attributes.get("userAttributes")).size());


        Assert.assertEquals("transactionGuid", error.get(5));
    }

    @Test
    public void testErrorEvents() {
        TelemetryBuffer data = new TelemetryBuffer();
        int eventsSeen = 1;
        int reservoirSize = 111;

        data.updateErrorReservoirSize(reservoirSize);
        data.updateErrorEventsSeen(eventsSeen);

        Collection<ErrorEvent> errorEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");
        Map<String, String> syntheticsAttr = new HashMap<>();
        syntheticsAttr.put("sAttr", "sVal");
        Map<String, Object> dTIntrinsics = new HashMap<>();
        dTIntrinsics.put("dAttr", "dVal");
        Map<String, Object> agentAttr = new HashMap<>();
        agentAttr.put("aAttr", "aVal");

        ErrorEvent errorEvent = new ErrorEvent("appName", 10L, 1.05f, userAttributes, "MyClass", "message",
                true, "txnName",
                111, 2, 3, 4, 5, 6, 7,
                "txnGuid", "refTxnGuid", "syntheticsResourceId",
                "sm", "sj", "st", "si", syntheticsAttr, 8080,
                "cause", "tripId", dTIntrinsics, agentAttr, new AttributeFilter.PassEverythingAttributeFilter());

        errorEvents.add(errorEvent);

        data.updateErrorEvents(errorEvents);

        JSONObject formatted = data.formatJson();
        Assert.assertTrue(formatted.containsKey("error_event_data"));
        JSONArray list = (JSONArray) formatted.get("error_event_data");
        Assert.assertEquals(3, list.size());
        Assert.assertNull(list.get(0));
        JSONObject eventInfo = (JSONObject) list.get(1);
        Assert.assertEquals(2, eventInfo.size());
        Assert.assertEquals(1, eventInfo.get("events_seen"));
        Assert.assertEquals(111, eventInfo.get("reservoir_size"));

        JSONArray errors = (JSONArray) list.get(2);
        JSONArray formattedEvent = (JSONArray) errors.get(0);

        Map<String, Object> formattedIntrinsics = (Map<String, Object>) formattedEvent.get(0);
        Assert.assertEquals("TransactionError", formattedIntrinsics.get("type"));
        Assert.assertEquals("dVal", formattedIntrinsics.get("dAttr"));

        Map<String, Object> formattedUserAttrs = (Map<String, Object>) formattedEvent.get(1);
        Assert.assertEquals("uVal", formattedUserAttrs.get("uAttr"));

        Map<String, Object> formattedAgentAttr = (Map<String, Object>) formattedEvent.get(2);
        Assert.assertEquals("aVal", formattedAgentAttr.get("aAttr"));

    }

    @Test
    public void testSpanEvents() {
        TelemetryBuffer data = new TelemetryBuffer();
        int eventsSeen = 1;
        int reservoirSize = 111;
        data.updateSpanReservoirSize(reservoirSize);
        data.updateSpanEventsSeen(eventsSeen);
        Collection<SpanEvent> spanEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");
        SpanEvent spanEvent = SpanEvent.builder()
                .appName("myAppName")
                .priority(1.0f)
                .putAgentAttribute("aAttr", "aVal")
                .spanKind("producer")
                .putIntrinsic("iAttr", "iVal")
                .putAllUserAttributes(userAttributes)
                .timestamp(11232344)
                .build();

        spanEvents.add(spanEvent);
        data.updateSpanEvents(spanEvents);

        JSONObject formatted = data.formatJson();
        Assert.assertTrue(formatted.containsKey("span_event_data"));
        JSONArray list = (JSONArray) formatted.get("span_event_data");

        Assert.assertNull(list.get(0));
        JSONObject eventInfo = (JSONObject) list.get(1);
        Assert.assertEquals(2, eventInfo.size());
        Assert.assertEquals(1, eventInfo.get("events_seen"));
        Assert.assertEquals(111, eventInfo.get("reservoir_size"));

        JSONArray errors = (JSONArray) list.get(2);
        JSONArray formattedEvent = (JSONArray) errors.get(0);

        Map<String, Object> formattedIntrinsics = (Map<String, Object>) formattedEvent.get(0);
        Assert.assertEquals("Span", formattedIntrinsics.get("type"));
        Assert.assertEquals("producer", formattedIntrinsics.get("span.kind"));
        Assert.assertEquals("iVal", formattedIntrinsics.get("iAttr"));

        Map<String, Object> formattedUserAttrs = (Map<String, Object>) formattedEvent.get(1);
        Assert.assertEquals("uVal", formattedUserAttrs.get("uAttr"));

        Map<String, Object> formattedAgentAttr = (Map<String, Object>) formattedEvent.get(2);
        Assert.assertEquals("aVal", formattedAgentAttr.get("aAttr"));
    }

    @Test
    public void testAnalyticEvents() {
        TelemetryBuffer data = new TelemetryBuffer();

        data.updateAnalyticEventsSeen(1);
        data.updateAnalyticReservoirSize(111);
        addTransactionEvent(data);

        data.updateCustomEventsSeen(1);
        data.updateCustomEventsReservoirSize(24);
        addCustomEvent(data);

        data.updateLogEventsSeen(1);
        data.updateLogEventsReservoir(5);
        addLogEvent(data);


        JSONObject formatted = data.formatJson();
        Assert.assertTrue(formatted.containsKey("analytic_event_data"));
        JSONArray list = (JSONArray) formatted.get("analytic_event_data");

        Assert.assertNull(list.get(0));
        JSONObject eventInfo = (JSONObject) list.get(1);

        Assert.assertEquals(2, eventInfo.size());
        Assert.assertEquals(3, eventInfo.get("events_seen"));
        Assert.assertEquals(140, eventInfo.get("reservoir_size"));

        JSONArray events = (JSONArray) list.get(2);

        JSONArray transactionEvent = (JSONArray) events.get(0);
        JSONArray customEvent = (JSONArray) events.get(1);
        JSONArray logEvent = (JSONArray) events.get(2);
        assertTransactionEvent(transactionEvent);
        assertCustomEvent(customEvent);
        assertLogEvent(logEvent);
    }

    private void addTransactionEvent(TelemetryBuffer data) {
        Collection<TransactionEvent> transactionEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");

        Map<String, String> syntheticsAttr = new HashMap<>();
        syntheticsAttr.put("sAttr", "sVal");

        Map<String, Object> dtIntrinsics = new HashMap<>();
        dtIntrinsics.put("dAttr", "dVal");

        TransactionEvent transactionEvent = new TransactionEventBuilder()
                .setAppName("appName")
                .setTimestamp(1414523453253251212L)
                .setName("txnName")
                .setDuration(10000)
                .setGuid("asdad")
                .setReferringGuid("ewqadads")
                .setPort(8081)
                .setTripId("adadadsa")
                .setApdexPerfZone(ApdexPerfZone.SATISFYING)
                .setError(false)
                .setpTotalTime(1001)
                .setTimeoutCause(null)
                .setPriority(.25F)
                .putAllUserAttributes(userAttributes)
                .setDatabase(new CountedDuration(1, 3))
                .setExternal(new CountedDuration(2, 5))
                .setQueueDuration(23)
                .setGcCumulative(1100)
                .setTimeToFirstByte(1)
                .setTimeToLastByte(2)
                .setDistributedTraceIntrinsics(dtIntrinsics)
                .setSyntheticsIds(new SyntheticsIds("a", "b", "c"))
                .setSyntheticsInfo(new SyntheticsInfo("someVal", "myType", syntheticsAttr))
                .setTimeoutCause(TimeoutCause.SEGMENT)
                .setPriority(0.03f)
                .build();

        Map<String, Object> agentAttr = new HashMap<>();
        agentAttr.put("aAttr", "aVal");

        transactionEvent.setAgentAttributes(agentAttr);

        transactionEvents.add(transactionEvent);
        data.updateAnalyticEvents(transactionEvents);
    }

    private void assertTransactionEvent(JSONArray event) {
        Map<String, Object> intrinsicAttrs = (Map<String, Object>) event.get(0);
        Map<String, Object> userAttr = (Map<String, Object>) event.get(1);
        Map<String, Object> agentAttr = (Map<String, Object>) event.get(2);

        Assert.assertEquals(10000.0f, (float)intrinsicAttrs.get("duration"), 0.0f);
        Assert.assertEquals("dVal", intrinsicAttrs.get("dAttr"));
        Assert.assertEquals(1.0f, (float)intrinsicAttrs.get("databaseDuration"), 0.0f);
        Assert.assertEquals("txnName", intrinsicAttrs.get("name"));
        Assert.assertEquals("Transaction", intrinsicAttrs.get("type"));
        Assert.assertEquals(1414523453253251212L, intrinsicAttrs.get("timestamp"));

        Assert.assertEquals("uVal", userAttr.get("uAttr"));
        Assert.assertEquals("aVal", agentAttr.get("aAttr"));
    }


    private void addCustomEvent(TelemetryBuffer data) {
        Collection<AnalyticsEvent> customEvents = new ArrayList<>();

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("uAttr", "uVal");

        AnalyticsEvent customInsightsEvent = Mockito.mock(CustomInsightsEvent.class);
        Mockito.when(customInsightsEvent.getUserAttributesCopy()).thenReturn(userAttributes);
        Mockito.when(customInsightsEvent.getType()).thenReturn("Custom");
        customEvents.add(customInsightsEvent);

        data.updateAnalyticEvents(customEvents);
    }

    private void assertCustomEvent(JSONArray event) {
        Map<String, Object> intrinsicAttrs = (Map<String, Object>) event.get(0);
        Map<String, Object> userAttr = (Map<String, Object>) event.get(1);
        Map<String, Object> agentAttr = (Map<String, Object>) event.get(2);

        Assert.assertEquals("Custom", intrinsicAttrs.get("type"));
        Assert.assertEquals(0, agentAttr.size());
        Assert.assertEquals("uVal", userAttr.get("uAttr"));
    }


    private void addLogEvent(TelemetryBuffer data) {
        Collection<LogEvent> logEvents = new ArrayList<>();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", "val");
        LogEvent logEvent = new LogEvent(attrs, 0.332f);
        logEvents.add(logEvent);
        data.updateAnalyticEvents(logEvents);
    }

    private void assertLogEvent(JSONArray event) {
        Map<String, Object> intrinsicAttrs = (Map<String, Object>) event.get(0);
        Map<String, Object> userAttr = (Map<String, Object>) event.get(1);
        Map<String, Object> agentAttr = (Map<String, Object>) event.get(2);

        Assert.assertEquals("LogEvent", intrinsicAttrs.get("type"));
        Assert.assertEquals(0, agentAttr.size());
        Assert.assertEquals("val", userAttr.get("attr"));
    }

    @Test
    public void testMetrics() {
        TelemetryBuffer data = new TelemetryBuffer();
        MetricData metricData = MetricData.create(
                MetricName.create("Other/myMetric", "myScope"),
                101,
                new StatsImpl(5, 2, 0, 3, 2));
        data.updateMetricBeginTimeMillis(1L);
        data.updateMetricEndTimeMillis(2L);
        data.updateMetricData(Arrays.asList(metricData));
        JSONObject formatted = data.formatJson();
        JSONArray list = (JSONArray) formatted.get("metric_data");
        Assert.assertNull(list.get(0));
        Assert.assertEquals(1L, list.get(1));
        Assert.assertEquals(2L, list.get(2));

        JSONArray metricArray = (JSONArray) ((JSONArray)list.get(3)).get(0);
        JSONObject metricInfo = (JSONObject) metricArray.get(0);
        Assert.assertEquals("Other/myMetric", metricInfo.get("name"));
        Assert.assertEquals("myScope", metricInfo.get("scope"));
        JSONArray statsJson = (JSONArray) metricArray.get(1);
        Assert.assertEquals(5, statsJson.get(0));
        Assert.assertEquals(2.0f, (float) statsJson.get(1), 0.0);
        Assert.assertEquals(2.0f, (float) statsJson.get(2), 0.0);
        Assert.assertEquals(3.0f, (float) statsJson.get(3), 0.0);
        Assert.assertEquals(0.0f, (float) statsJson.get(4), 0.0);
        Assert.assertEquals(2.0, (double) statsJson.get(5), 0.0);

    }

    @Test
    public void testMetricChanges() {
        TelemetryBuffer data = new TelemetryBuffer();
        Assert.assertEquals((Long) 0L, data.getMetricBeginTimeMillis());
        Assert.assertEquals((Long) 0L, data.getMetricEndTimeMillis());

        data.updateMetricBeginTimeMillis(100L);
        data.updateMetricEndTimeMillis(200L);
        Assert.assertEquals((Long) 100L, data.getMetricBeginTimeMillis());
        Assert.assertEquals((Long) 200L, data.getMetricEndTimeMillis());

        data.updateMetricBeginTimeMillis(120L);
        data.updateMetricEndTimeMillis(150L);
        Assert.assertEquals((Long) 100L, data.getMetricBeginTimeMillis());
        Assert.assertEquals((Long) 200L, data.getMetricEndTimeMillis());

        data.updateMetricBeginTimeMillis(50L);
        data.updateMetricEndTimeMillis(300L);
        Assert.assertEquals((Long) 50L, data.getMetricBeginTimeMillis());
        Assert.assertEquals((Long) 300L, data.getMetricEndTimeMillis());
    }

    @Test
    public void testTransactionTraces() {
        TelemetryBuffer data = new TelemetryBuffer();
        TransactionTrace trace = createTransactionTrace();
        data.updateTransactionTraces(Collections.singletonList(trace));
        assertTransactionTrace(data);
    }

    private TransactionTrace createTransactionTrace() {
        TransactionTrace trace = Mockito.mock(TransactionTrace.class);
        Mockito.when(trace.getStartTime()).thenReturn(111L);
        Mockito.when(trace.getDuration()).thenReturn(1000L);
        Mockito.when(trace.getRootMetricName()).thenReturn("Other/txn");
        Mockito.when(trace.getRequestUri()).thenReturn("localhost:8080");

        TransactionSegment segment = Mockito.mock(TransactionSegment.class);
        Mockito.when(segment.getStartTime()).thenReturn(111L);
        Mockito.when(segment.getEndTime()).thenReturn(1111L);
        Mockito.when(segment.getMetricName()).thenReturn("ROOT");

        Map filteredAttrs = new HashMap<>();
        filteredAttrs.put("fAttr", "fVal");
        Mockito.when(segment.getFilteredAttributes()).thenReturn(filteredAttrs);

        TransactionSegment childSegment = Mockito.mock(TransactionSegment.class);
        Mockito.when(childSegment.getStartTime()).thenReturn(200L);
        Mockito.when(childSegment.getEndTime()).thenReturn(300L);
        Mockito.when(childSegment.getMetricName()).thenReturn("childValue");
        Mockito.when(childSegment.getChildren()).thenReturn(Collections.emptyList());
        Mockito.when(childSegment.getClassName()).thenReturn("some.childClass");
        Mockito.when(childSegment.getMethodName()).thenReturn("myChildMethod");
        Mockito.when(childSegment.getFilteredAttributes()).thenReturn(Collections.EMPTY_MAP);

        Mockito.when(segment.getChildren()).thenReturn(Collections.singletonList(childSegment));
        Mockito.when(segment.getClassName()).thenReturn("some.className");
        Mockito.when(segment.getMethodName()).thenReturn("getValue");

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", "val");
        Mockito.when(trace.getTraceDetailsAsList()).thenReturn(Arrays.asList(111L,
                Collections.EMPTY_MAP, Collections.EMPTY_MAP, segment, attrs));
        Mockito.when(trace.getGuid()).thenReturn("guid");
        Mockito.when(trace.getSyntheticsResourceId()).thenReturn("resourceId");
        return trace;
    }

    private void assertTransactionTrace(TelemetryBuffer data) {
        JSONObject formatted = data.formatJson();
        JSONArray list = (JSONArray) formatted.get("transaction_sample_data");
        Assert.assertNull(list.get(0));
        JSONArray traceJson = (JSONArray) ((JSONArray) list.get(1)).get(0);
        Assert.assertEquals(111L, traceJson.get(0));
        Assert.assertEquals(1000L, traceJson.get(1));
        Assert.assertEquals("Other/txn", traceJson.get(2));
        Assert.assertEquals("localhost:8080", traceJson.get(3));
        JSONArray traceDetailsJson = (JSONArray) traceJson.get(4);

        Assert.assertEquals(111L, traceDetailsJson.get(0));
        Assert.assertEquals(0, ((Map<?, ?>)traceDetailsJson.get(1)).size());
        Assert.assertEquals(0, ((Map<?, ?>)traceDetailsJson.get(2)).size());
        JSONArray segment = (JSONArray) traceDetailsJson.get(3);
        assertSegment(segment);
        Assert.assertEquals("val", ((Map<?, ?>)traceDetailsJson.get(4)).get("attr"));
        Assert.assertEquals("guid", traceJson.get(5));
        Assert.assertEquals(null, traceJson.get(6));
        Assert.assertEquals(false, traceJson.get(7));
        Assert.assertEquals(null, traceJson.get(8));
        Assert.assertEquals("resourceId", traceJson.get(9));
    }

    private void assertSegment(JSONArray segment) {
        Assert.assertEquals(111L, segment.get(0));
        Assert.assertEquals(1111L, segment.get(1));
        Assert.assertEquals("ROOT", segment.get(2));
        Assert.assertEquals("fVal", ((Map)segment.get(3)).get("fAttr"));
        JSONArray children = (JSONArray) segment.get(4);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals("some.className", segment.get(5));
        Assert.assertEquals("getValue", segment.get(6));

        JSONArray childSegment = (JSONArray) children.get(0);
        Assert.assertEquals(200L, childSegment.get(0));
        Assert.assertEquals(300L, childSegment.get(1));
        Assert.assertEquals("childValue", childSegment.get(2));
        Assert.assertEquals(0, ((Map)childSegment.get(3)).size());
        Assert.assertEquals(0, ((JSONArray) childSegment.get(4)).size());
        Assert.assertEquals("some.childClass", childSegment.get(5));
        Assert.assertEquals("myChildMethod", childSegment.get(6));
    }

    @Test
    public void testSqlTraces() throws IOException {
        TelemetryBuffer data = new TelemetryBuffer();
        SqlTrace sqlTrace = Mockito.mock(SqlTrace.class);
        Mockito.when(sqlTrace.getBlameMetricName()).thenReturn("blameMetricName");
        Mockito.when(sqlTrace.getUri()).thenReturn("uri");
        Mockito.when(sqlTrace.getId()).thenReturn(111L);
        Mockito.when(sqlTrace.getQuery()).thenReturn("query");
        Mockito.when(sqlTrace.getMetricName()).thenReturn("metricName");
        Mockito.when(sqlTrace.getCallCount()).thenReturn(10);
        Mockito.when(sqlTrace.getTotal()).thenReturn(3L);
        Mockito.when(sqlTrace.getMin()).thenReturn(1L);
        Mockito.when(sqlTrace.getMax()).thenReturn(5L);
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        Mockito.when(sqlTrace.getParameters()).thenReturn(params);
        data.updateSqlTraces(Collections.singletonList(sqlTrace));

        JSONObject formatted = data.formatJson();
        JSONArray sqlTraceJson = (JSONArray) ((JSONArray) ((JSONArray)formatted.get("sql_trace_data")).get(0)).get(0);
        Assert.assertEquals("blameMetricName", sqlTraceJson.get(0));
        Assert.assertEquals("uri", sqlTraceJson.get(1));
        Assert.assertEquals(111L, sqlTraceJson.get(2));
        Assert.assertEquals("query", sqlTraceJson.get(3));
        Assert.assertEquals("metricName", sqlTraceJson.get(4));
        Assert.assertEquals(10, sqlTraceJson.get(5));
        Assert.assertEquals(3L, sqlTraceJson.get(6));
        Assert.assertEquals(1L, sqlTraceJson.get(7));
        Assert.assertEquals(5L, sqlTraceJson.get(8));

        String compressedAndEncodedParams = (String) sqlTraceJson.get(9);
        Assert.assertEquals("H4sIAAAAAAAAAKtWyk6tVLJSKkvMKU1VqgUAv5wYPw8AAAA=", compressedAndEncodedParams);

        byte[] decodedParams = Base64.getDecoder().decode(compressedAndEncodedParams);
        GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(decodedParams));
        byte[] jsonUnzipped = new byte[decodedParams.length];
        int res = inputStream.read(jsonUnzipped);
        Assert.assertNotEquals(-1, res);

        String paramsJsonStr = new String(jsonUnzipped).trim();
        Assert.assertEquals("{\"key\":\"value\"}", paramsJsonStr);

    }
}

