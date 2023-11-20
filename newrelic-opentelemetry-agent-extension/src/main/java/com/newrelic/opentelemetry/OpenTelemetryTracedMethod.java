/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.opentelemetry;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.TracedMethod;
import io.opentelemetry.api.trace.Span;

import java.util.Map;

final class OpenTelemetryTracedMethod implements TracedMethod {

    private static final OpenTelemetryTracedMethod INSTANCE = new OpenTelemetryTracedMethod();

    private OpenTelemetryTracedMethod() {
    }

    static OpenTelemetryTracedMethod getInstance() {
        return INSTANCE;
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
        Span span = Span.current();
        if (value instanceof Double || value instanceof Float) {
            span.setAttribute(key, value.doubleValue());
        } else {
            span.setAttribute(key, value.intValue());
        }
    }

    @Override
    public void addCustomAttribute(String key, String value) {
        Span.current().setAttribute(key, value);
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
        Span.current().setAttribute(key, value);
    }

    @Override
    public void addCustomAttributes(Map<String, Object> attributes) {
        Span.current().setAllAttributes(OpenTelemetryNewRelic.toAttributes(attributes).build());
    }

    @Override
    public String getMetricName() {
        OpenTelemetryNewRelic.logUnsupportedMethod("TracedMethod", "getMetricName");
        return "NoAgent";
    }

    @Override
    public void setMetricName(String... metricNameParts) {
        OpenTelemetryNewRelic.logUnsupportedMethod("TracedMethod", "setMetricName");
    }

    @Override
    public void addRollupMetricName(String... metricNameParts) {
        OpenTelemetryNewRelic.logUnsupportedMethod("TracedMethod", "addRollupMetricName");
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
        OpenTelemetryNewRelic.logUnsupportedMethod("TracedMethod", "reportAsExternal");
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        OpenTelemetryNewRelic.logUnsupportedMethod("TracedMethod", "addOutboundRequestHeaders");
    }

}
