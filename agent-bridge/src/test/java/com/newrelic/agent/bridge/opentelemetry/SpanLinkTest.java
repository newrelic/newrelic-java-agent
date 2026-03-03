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

public class SpanLinkTest extends TestCase {
    long timestamp = System.currentTimeMillis();
    String type = "SpanLink";
    String id = "id";
    String traceId = "traceId";
    String linkedSpanId = "linkedSpanId";
    String linkedTraceId = "linkedTraceId";
    Map<String, Object> userAttributes = Collections.singletonMap("foo", "bar");
    SpanLink spanLink = new SpanLink(timestamp, id, traceId, linkedSpanId, linkedTraceId, userAttributes);

    public void testGetType() {
        assertEquals(type, spanLink.getType());
    }

    public void testGetTimestamp() {
        assertEquals(timestamp, spanLink.getTimestamp());
    }

    public void testGetId() {
        assertEquals(id, spanLink.getId());
    }

    public void testGetTraceId() {
        assertEquals(traceId, spanLink.getTraceId());
    }

    public void testGetLinkedSpanId() {
        assertEquals(linkedSpanId, spanLink.getLinkedSpanId());
    }

    public void testGetLinkedTraceId() {
        assertEquals(linkedTraceId, spanLink.getLinkedTraceId());
    }

    public void testGetUserAttributes() {
        assertEquals(userAttributes, spanLink.getUserAttributes());
    }
}
