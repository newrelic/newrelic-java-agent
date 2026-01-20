/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.EventOnSpan;

import java.util.Map;
import java.util.function.Supplier;

import static com.newrelic.agent.json.AttributeFilters.SPAN_EVENTS_ATTRIBUTE_FILTER;
import static com.newrelic.agent.model.EventOnSpan.SPAN_EVENT;

/**
 * This wraps up the logic involved in creating an instance of a EventOnSpan event.
 */
public class EventOnSpanFactory {
    public static final Supplier<Long> DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER = System::currentTimeMillis;

    private final EventOnSpan.Builder builder = EventOnSpan.builder();
    private final String appName;
    private final AttributeFilter filter;
    private final Supplier<Long> timestampSupplier;

    public EventOnSpanFactory(String appName, AttributeFilter filter, Supplier<Long> timestampSupplier) {
        this.filter = filter;
        builder.putIntrinsic("type", SPAN_EVENT);
        this.appName = appName;
        this.timestampSupplier = timestampSupplier;
        builder.appName(appName);
    }

    public EventOnSpanFactory(String appName) {
        this(appName, SPAN_EVENTS_ATTRIBUTE_FILTER, DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
    }

    public EventOnSpanFactory setTimestamp(long startTime) {
        builder.putIntrinsic("timestamp", startTime);
        return this;
    }

    public EventOnSpanFactory setSpanId(String spanId) {
        builder.putIntrinsic("span.id", spanId);
        return this;
    }

    public EventOnSpanFactory setTraceId(String traceId) {
        builder.putIntrinsic("trace.id", traceId);
        return this;
    }

    public EventOnSpanFactory setName(String name) {
        builder.putIntrinsic("name", name);
        return this;
    }

    public EventOnSpanFactory setUserAttributes(Map<String, ?> userAttributes) {
        builder.putAllUserAttributes(filter.filterUserAttributes(appName, userAttributes));
        return this;
    }

    public EventOnSpanFactory putAllUserAttributes(Map<String, ?> userAttributes) {
        userAttributes = filter.filterUserAttributes(appName, userAttributes);
        builder.putAllUserAttributes(userAttributes);
        return this;
    }

    public EventOnSpanFactory putAllUserAttributesIfAbsent(Map<String, ?> userAttributes) {
        builder.putAllUserAttributesIfAbsent(filter.filterUserAttributes(appName, userAttributes));
        return this;
    }

    public EventOnSpan build() {
        builder.timestamp(timestampSupplier.get());
        return builder.build();
    }
}
