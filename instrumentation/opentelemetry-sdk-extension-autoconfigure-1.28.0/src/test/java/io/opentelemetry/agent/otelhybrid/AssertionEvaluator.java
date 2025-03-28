/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.opentelemetry.agent.otelhybrid;

import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;

import java.util.Map;

import static org.junit.Assert.*;

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