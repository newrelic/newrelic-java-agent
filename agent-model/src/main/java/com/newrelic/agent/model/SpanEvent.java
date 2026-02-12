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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpanEvent extends AnalyticsEvent implements JSONStreamAware {

    public static final String SPAN = "Span";
    static final String CLIENT_SPAN_KIND = "client";

    private final String appName;
    private final Map<String, Object> intrinsics;
    private Map<String, Object> agentAttributes;

    // this is the list of attributes used for entity synthesis on the backend
    // when doing partial granularity tracing these attrs should be kept on spans for that purpose
    // all other agent attrs (and custom attributes) will be removed
    public final static Set<String> ENTITY_SYNTHESIS_ATTRS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "cloud.account.id",
            "cloud.platform",
            "cloud.region",
            "cloud.resource_id",
            "db.instance",
            "db.system",
            "http.url",
            "messaging.destination.name",
            "messaging.system",
            "peer.hostname",
            "server.address",
            "server.port",
            "span.kind")));

    // these should also be kept during partial granularity sampling,
    // but only if at least 1 entity synthesis attr is present
    public final static Set<String> ERROR_ATTRS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "error.class",
            "error.message",
            "error.expected")));

    public final static Set<String> ESSENTIAL_ATTRIBUTES = Collections.unmodifiableSet(
            Stream.concat(ENTITY_SYNTHESIS_ATTRS.stream(), ERROR_ATTRS.stream())
            .collect(Collectors.toSet()));

    private final List<LinkOnSpan> linkOnSpanEvents;
    private final List<EventOnSpan> eventOnSpanEvents;

    private SpanEvent(Builder builder) {
        super(SPAN, builder.timestamp, builder.priority, builder.userAttributes);
        this.appName = builder.appName;
        this.agentAttributes = builder.agentAttributes;
        this.intrinsics = builder.intrinsics;
        this.linkOnSpanEvents = builder.linkOnSpanEvents;
        this.eventOnSpanEvents = builder.eventOnSpanEvents;
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

    public boolean shouldBeKeptForPartialGranularity() {
        // should be kept if it's either an LLM span, or it has entity synthesis attributes
        if (getName() != null && getName().toUpperCase().startsWith("LLM")) return true;

        return hasAnyEntitySynthAttrs();
    }

    public void updateParentSpanId(String newId) {
        intrinsics.put("parentId", newId);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(intrinsics, getMutableUserAttributes(), getAgentAttributes()), out);
    }

    public List<LinkOnSpan> getLinkOnSpanEvents() {
        return linkOnSpanEvents;
    }

    public List<EventOnSpan> getEventOnSpanEvents() {
        return eventOnSpanEvents;
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

    public Long getStartTimestamp() {
        return (Long) intrinsics.get("timestamp");
    }

    public SpanCategory getCategory() {
        return SpanCategory.fromString((String) intrinsics.get("category"));
    }

    public boolean matchesEntitySynthesisAttrs(SpanEvent otherSpan) {
        for (String attr : ENTITY_SYNTHESIS_ATTRS) {
            if (!Objects.equals(getAgentAttributes().get(attr), otherSpan.getAgentAttributes().get(attr))) return false;
        }

        return true;
    }
    public boolean hasAnyErrorAttrs() {
        for (String attr : ERROR_ATTRS) {
            if (getAgentAttributes().containsKey(attr)) return true;
        }
        return false;
    }

    public boolean hasAnyEntitySynthAttrs() {
        if (getAgentAttributes() == null || getAgentAttributes().size() == 0) return false;
        for (String attr : ENTITY_SYNTHESIS_ATTRS) {
            if (getAgentAttributes().containsKey(attr)) return true;
        }
        return false;
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
        return Objects.equals(appName, spanEvent.appName) &&
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
        private List<LinkOnSpan> linkOnSpanEvents = new ArrayList<>();
        private List<EventOnSpan> eventOnSpanEvents = new ArrayList<>();
        private String appName;
        private float priority;
        private long timestamp;
        private Object spanKind;

        private boolean removeNonEssentialAttrs = false;

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder priority(float priority) {
            this.priority = priority;
            return this;
        }

        public Builder linkOnSpanEvents(List<LinkOnSpan> linkOnSpanEvents) {
            if (linkOnSpanEvents != null) {
                this.linkOnSpanEvents = linkOnSpanEvents;
            }
            return this;
        }

        public Builder eventOnSpanEvents(List<EventOnSpan> eventOnSpanEvents) {
            if (eventOnSpanEvents != null) {
                this.eventOnSpanEvents = eventOnSpanEvents;
            }
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
            if (agentAttributes == null) return this;

            for (String attr : agentAttributes.keySet()) {
                putAgentAttribute(attr, agentAttributes.get(attr));
            }
            return this;
        }

        public Builder putAllUserAttributes(Map<String, ?> userAttributes) {
            if (removeNonEssentialAttrs) return this; // no user attributes for partial granularity
            if (userAttributes == null || userAttributes.isEmpty()) {
                return this;
            }
            for (Map.Entry<String, ?> entry : userAttributes.entrySet()) {
                this.userAttributes.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder putAllUserAttributesIfAbsent(Map<String, ?> userAttributes) {
            if (removeNonEssentialAttrs) return this; // no user attributes for partial granularity
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
                if (!removeNonEssentialAttrs || ESSENTIAL_ATTRIBUTES.contains(key)) { // only add essential attributes if doing partial granularity
                    this.agentAttributes.put(key, value);
                }
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

        public Builder removeNonEssentialAttrs(boolean removeNonEssentialAttrs) {
            this.removeNonEssentialAttrs = removeNonEssentialAttrs;
            return this;
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
