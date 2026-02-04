/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.LinkOnSpan;

import java.util.Map;
import java.util.function.Supplier;

import static com.newrelic.agent.json.AttributeFilters.SPAN_EVENTS_ATTRIBUTE_FILTER;
import static com.newrelic.agent.model.LinkOnSpan.SPAN_LINK;

/**
 * This wraps up the logic involved in creating an instance of a LinkOnSpan event.
 */
public class LinkOnSpanFactory {
    public static final Supplier<Long> DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER = System::currentTimeMillis;

    private final LinkOnSpan.Builder builder = LinkOnSpan.builder();
    private final String appName;
    private final AttributeFilter filter;
    private final Supplier<Long> timestampSupplier;

    public LinkOnSpanFactory(String appName, AttributeFilter filter, Supplier<Long> timestampSupplier) {
        this.filter = filter;
        builder.putIntrinsic("type", SPAN_LINK);
        this.appName = appName;
        this.timestampSupplier = timestampSupplier;
        builder.appName(appName);
    }

    public LinkOnSpanFactory(String appName) {
        this(appName, SPAN_EVENTS_ATTRIBUTE_FILTER, DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
    }

    public LinkOnSpanFactory setTimestamp(long startTime) {
        builder.putIntrinsic("timestamp", startTime);
        return this;
    }

    public LinkOnSpanFactory setId(String id) {
        builder.putIntrinsic("id", id);
        return this;
    }

    public LinkOnSpanFactory setTraceId(String traceId) {
        builder.putIntrinsic("trace.id", traceId);
        return this;
    }

    public LinkOnSpanFactory setLinkedSpanId(String linkedSpanId) {
        builder.putIntrinsic("linkedSpanId", linkedSpanId);
        return this;
    }

    public LinkOnSpanFactory setLinkedTraceId(String linkedTraceId) {
        builder.putIntrinsic("linkedTraceId", linkedTraceId);
        return this;
    }

    public LinkOnSpanFactory setUserAttributes(Map<String, ?> userAttributes) {
        builder.putAllUserAttributes(filter.filterUserAttributes(appName, userAttributes));
        return this;
    }

    public LinkOnSpanFactory putAllUserAttributes(Map<String, ?> userAttributes) {
        userAttributes = filter.filterUserAttributes(appName, userAttributes);
        builder.putAllUserAttributes(userAttributes);
        return this;
    }

    public LinkOnSpanFactory putAllUserAttributesIfAbsent(Map<String, ?> userAttributes) {
        builder.putAllUserAttributesIfAbsent(filter.filterUserAttributes(appName, userAttributes));
        return this;
    }

    public LinkOnSpan build() {
        builder.timestamp(timestampSupplier.get());
        return builder.build();
    }
}
