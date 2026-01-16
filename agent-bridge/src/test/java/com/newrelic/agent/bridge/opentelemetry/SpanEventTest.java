/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.opentelemetry;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.Map;

public class SpanEventTest extends TestCase {
    long timestamp = System.currentTimeMillis();
    String type = "SpanEvent";
    String spanId = "spanId";
    String traceId = "traceId";
    String name = "name";
    Map<String, Object> userAttributes = Collections.singletonMap("foo", "bar");
    SpanEvent spanEvent = new SpanEvent(timestamp, name, traceId, spanId, userAttributes);

    public void testGetType() {
        assertEquals(type, spanEvent.getType());
    }

    public void testGetTimestamp() {
        assertEquals(timestamp, spanEvent.getTimestamp());
    }

    public void testGetId() {
        assertEquals(spanId, spanEvent.getSpanId());
    }

    public void testGetTraceId() {
        assertEquals(traceId, spanEvent.getTraceId());
    }

    public void testGetLinkedSpanId() {
        assertEquals(name, spanEvent.getName());
    }

    public void testGetUserAttributes() {
        assertEquals(userAttributes, spanEvent.getUserAttributes());
    }

}