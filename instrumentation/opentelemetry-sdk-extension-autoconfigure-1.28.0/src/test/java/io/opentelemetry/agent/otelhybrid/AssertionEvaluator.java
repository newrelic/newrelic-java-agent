/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.opentelemetry.agent.otelhybrid;

import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static io.opentelemetry.agent.otelhybrid.HybridAgentTest.PARENT_SPAN_ID;
import static io.opentelemetry.agent.otelhybrid.HybridAgentTest.PARENT_TRACE_ID;
import static io.opentelemetry.agent.otelhybrid.HybridAgentTest.SPAN_ID;
import static io.opentelemetry.agent.otelhybrid.HybridAgentTest.TRACE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AssertionEvaluator {
    public static void assertNoTxnExists(Introspector introspector) {
        assertEquals(0, introspector.getFinishedTransactionCount());
    }

    public static void assertNoNewRelicSpanExists(Introspector introspector) {
        assertEquals(0, introspector.getSpanEvents().size());
    }

    public static void assertSpanCount(Introspector introspector, int count) {
        assertEquals(count, introspector.getSpanEvents().size());
    }

    public static void assertSpanDetails(Introspector introspector, Map<String, String> otelSpanDetails) {
        Collection<SpanEvent> spanEvents = introspector.getSpanEvents();
        if (!spanEvents.isEmpty() && !otelSpanDetails.isEmpty()) {
            String otelSpanId = otelSpanDetails.get(SPAN_ID);
            String otelSpanTraceId = otelSpanDetails.get(TRACE_ID);
            String otelParentSpanId = otelSpanDetails.get(PARENT_SPAN_ID);
            String otelParentTraceId = otelSpanDetails.get(PARENT_TRACE_ID);

            if (otelSpanId != null && otelSpanTraceId != null) {
                Optional<SpanEvent> spanEventOptional = spanEvents.stream().filter(span -> otelSpanId.equals(span.getGuid())).findFirst();
                if (spanEventOptional.isPresent()) {
                    SpanEvent nrSpan = spanEventOptional.get();
                    // OpenTelemetry API and New Relic API report the same traceId
                    assertEquals(otelSpanTraceId, nrSpan.traceId());
                    // OpenTelemetry API and New Relic API report the same spanId
                    assertEquals(otelSpanId, nrSpan.getGuid());
                }
            }

            if (otelParentSpanId != null && otelParentTraceId != null) {
                Optional<SpanEvent> spanEventOptional = spanEvents.stream().filter(span -> otelParentSpanId.equals(span.getGuid())).findFirst();
                if (spanEventOptional.isPresent()) {
                    SpanEvent nrParentSpan = spanEventOptional.get();
                    // OpenTelemetry API and New Relic API report the same traceId
                    assertEquals(otelParentTraceId, nrParentSpan.traceId());
                    // OpenTelemetry API and New Relic API report the same spanId
                    assertEquals(otelParentSpanId, nrParentSpan.getGuid());
                }
            }
        }
    }

    public static void assertTxnExists(Introspector introspector, String name) {
        assertEquals(name, introspector.getTransactionNames().iterator().next());
    }

    public static void assertUserAttributeExists(String key, Object val, Map<String, Object> attributes) {
        assertEquals(attributes.get(key), val);
    }

    public static void assertExceptionExistsOnSpan(SpanEvent spanEvent, String errorMessage, String errorClass) {
        assertEquals(errorMessage, spanEvent.getAgentAttributes().get("error.message"));
        assertEquals(errorClass, spanEvent.getAgentAttributes().get("error.class"));
    }

    public static void assertCarrierContainsW3CTraceParent(Map<String, String> carrier) {
        assertNotNull(carrier.get("traceparent"));
    }
}