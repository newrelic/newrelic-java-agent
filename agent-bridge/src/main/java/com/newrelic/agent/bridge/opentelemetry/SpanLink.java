/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.opentelemetry;

import java.util.Collections;
import java.util.Map;

/**
 * Representation of an OpenTelemetry SpanLink.
 */
public class SpanLink {

    // If a timestamp is not available in the OTel spanlink data model, we can use the start time of the span containing the span link as the timestamp value. This timestamp must be in the expected New Relic timestamp format.
    private final long timestamp;

    // The span id of the span containing the span link.
    private final String id;

    // The trace id of the span containing the span link.
    private final String traceId;

    // The span id of the upstream span defined on the span link.
    private final String linkedSpanId;

    // The trace id of the upstream span defined on the span link.
    private final String linkedTraceId;

    // Map of attributes associated with the span link.
    private final Map<String, Object> userAttributes;

    public SpanLink(long timestamp, String id, String traceId, String linkedSpanId, String linkedTraceId, Map<String, Object> userAttributes) {
        this.timestamp = timestamp;
        this.id = id;
        this.traceId = traceId;
        this.linkedSpanId = linkedSpanId;
        this.linkedTraceId = linkedTraceId;
        this.userAttributes = userAttributes;
    }

    // This is the event type that should be stored in NRDB.
    public String getType() {
        return "SpanLink";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getId() {
        return id;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getLinkedSpanId() {
        return linkedSpanId;
    }

    public String getLinkedTraceId() {
        return linkedTraceId;
    }

    public Map<String, ?> getUserAttributes() {
        if (userAttributes == null || userAttributes.isEmpty()) {
            return Collections.emptyMap();
        }
        return userAttributes;
    }
}
