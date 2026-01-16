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
 * Representation of an OpenTelemetry Span event.
 */
public class SpanEvent {

    // If a timestamp is not available in the OTel span event data model, we can use the start time of the span containing the span event as the timestamp value. This timestamp must be in the expected New Relic timestamp format.
    private final long timestamp;

    // The name of the span event as defined on the OpenTelemetry span event.
    private final String name;

    // The trace id of the span containing the span event.
    private final String traceId;

    // The span id of the span containing the span event.
    private final String spanId;

    // Map of attributes associated with the span event.
    private final Map<String, Object> userAttributes;

    public SpanEvent(long timestamp, String name, String traceId, String spanId, Map<String, Object> userAttributes) {
        this.timestamp = timestamp;
        this.name = name;
        this.traceId = traceId;
        this.spanId = spanId;
        this.userAttributes = userAttributes;
    }

    // This is the event type that should be stored in NRDB.
    public String getType() {
        return "SpanEvent";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public Map<String, ?> getUserAttributes() {
        if (userAttributes == null || userAttributes.isEmpty()) {
            return Collections.emptyMap();
        }
        return userAttributes;
    }
}
