/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents the New Relic data model of a SpanEvent event.
 * <p>
 * <p>
 * SpanEvent events have the following JSON structure of intrinsics, user attributes, and agent attributes:
 * <p>
 *      [
 *        {
 *          "trace.id": "123",
 *          "span.id": "123",
 *          "name": "MyEvent",
 *          "type": "SpanEvent",
 *          "timestamp": 123
 *        },
 *        {
 *          "customUserAttribute1": "someVal1",
 *          "customUserAttribute2": "someVal2",
 *        },
 *        {}
 *      ]
 */
public class EventOnSpan extends AnalyticsEvent implements JSONStreamAware {
    public static final String SPAN_EVENT = "SpanEvent";

    private final String appName;
    private final Map<String, Object> intrinsics;
    private final Map<String, Object> agentAttributes;

    private EventOnSpan(Builder builder) {
        super(SPAN_EVENT, builder.timestamp, builder.priority, builder.userAttributes);
        this.appName = builder.appName;
        this.agentAttributes = builder.agentAttributes;
        this.intrinsics = builder.intrinsics;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> getIntrinsics() {
        return intrinsics;
    }

    public String getAppName() {
        return appName;
    }

    public Map<String, Object> getAgentAttributes() {
        return agentAttributes;
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(intrinsics, getMutableUserAttributes(), getAgentAttributes()), out);
    }

    public String getName() {
        return (String) intrinsics.get("name");
    }

    public String getTraceId() {
        return (String) intrinsics.get("trace.id");
    }

    public String getSpanId() {
        return (String) intrinsics.get("span.id");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventOnSpan eventOnSpan = (EventOnSpan) o;
        return Objects.equals(appName, eventOnSpan.appName) &&
                Objects.equals(intrinsics, eventOnSpan.intrinsics) &&
                Objects.equals(agentAttributes, eventOnSpan.agentAttributes) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, intrinsics, agentAttributes);
    }

    public static class Builder {
        private final Map<String, Object> intrinsics = new HashMap<>();
        private final Map<String, Object> agentAttributes = new HashMap<>();
        private final Map<String, Object> userAttributes = new HashMap<>();
        private String appName;
        private float priority;
        private long timestamp;

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder priority(float priority) {
            // priority isn't used on EventOnSpan events
            this.priority = priority;
            return this;
        }

        public Builder putIntrinsic(String key, Object value) {
            if (key != null && value != null) {
                intrinsics.put(key, value);
            }
            return this;
        }

        public Builder putAllIntrinsics(Map<String, ?> intrinsicAttributes) {
            this.intrinsics.putAll(intrinsicAttributes);
            return this;
        }

        public Builder putAllAgentAttributes(Map<String, ?> agentAttributes) {
            this.agentAttributes.putAll(agentAttributes);
            return this;
        }

        public Builder putAllUserAttributes(Map<String, ?> userAttributes) {
            if (userAttributes == null || userAttributes.isEmpty()) {
                return this;
            }
            this.userAttributes.putAll(userAttributes);
            return this;
        }

        public Builder putAllUserAttributesIfAbsent(Map<String, ?> userAttributes) {
            if (userAttributes == null || userAttributes.isEmpty()) {
                return this;
            }
            for (Map.Entry<String, ?> entry : userAttributes.entrySet()) {
                if (!this.userAttributes.containsKey(entry.getKey())) {
                    this.userAttributes.put(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public Builder putAgentAttribute(String key, Object value) {
            if (key != null && value != null) {
                this.agentAttributes.put(key, value);
            }
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public EventOnSpan build() {
            if (timestamp == 0) {
                timestamp = System.currentTimeMillis();
            }
            return new EventOnSpan(this);
        }
    }
}
