/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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

public class SpanEvent extends AnalyticsEvent implements JSONStreamAware {

    public static final String SPAN = "Span";
    static final String CLIENT_SPAN_KIND = "client";

    private final String appName;
    private final Map<String, Object> intrinsics;
    private final Map<String, Object> agentAttributes;

    private SpanEvent(Builder builder) {
        super(SPAN, builder.timestamp, builder.priority, builder.userAttributes);
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

    public String getTraceId() {
        return (String) intrinsics.get("traceId");
    }

    public String getGuid() {
        return (String) intrinsics.get("guid");
    }

    public String getParentId() {
        return (String) intrinsics.get("parentId");
    }

    public String getName() {
        return (String) intrinsics.get("name");
    }

    public float getDuration() {
        return (Float) intrinsics.get("duration");
    }

    public String getTransactionId() {
        return (String) intrinsics.get("transactionId");
    }

    public SpanCategory getCategory() {
        return SpanCategory.fromString((String) intrinsics.get("category"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SpanEvent spanEvent = (SpanEvent) o;
        return  Objects.equals(appName, spanEvent.appName) &&
                Objects.equals(intrinsics, spanEvent.intrinsics) &&
                Objects.equals(agentAttributes, spanEvent.agentAttributes) &&
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
        private Object spanKind;

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder priority(float priority) {
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
            for (Map.Entry<String, ?> entry : userAttributes.entrySet()) {
                this.userAttributes.put(entry.getKey(), entry.getValue());
            }
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

        public Builder spanKind(Object spanKind) {
            putIntrinsic("span.kind", spanKind);
            this.spanKind = spanKind;
            return this;
        }

        public boolean isClientSpan() {
            return CLIENT_SPAN_KIND.equals(spanKind);
        }

        public Object getSpanKindFromUserAttributes() {
            Object result = userAttributes.get("span.kind");
            return result == null ? CLIENT_SPAN_KIND : result;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SpanEvent build() {
            if (timestamp == 0) {
                timestamp = System.currentTimeMillis();
            }
            return new SpanEvent(this);
        }
    }
}
