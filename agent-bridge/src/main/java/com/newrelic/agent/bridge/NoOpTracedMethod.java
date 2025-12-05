/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.agent.bridge.opentelemetry.SpanLink;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

import java.util.List;
import java.util.Map;

public final class NoOpTracedMethod implements TracedMethod {

    public static final TracedMethod INSTANCE = new NoOpTracedMethod();

    private NoOpTracedMethod() {
    }

    @Override
    public void setMetricName(String... metricNameParts) {
    }

    @Override
    public void nameTransaction(TransactionNamePriority namePriority) {
    }

    @Override
    public void addSpanLink(SpanLink link) {
    }

    @Override
    public List<SpanLink> getSpanLinks() {
        return null;
    }

    @Override
    public TracedMethod getParentTracedMethod() {
        return null;
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
    }

    @Override
    public void addCustomAttribute(String key, String value) {
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
    }

    @Override
    public void addCustomAttributes(Map<String, Object> attributes) {
    }

    @Override
    public String getMetricName() {
        return "NoOpTracedMethod";
    }

    @Override
    public void setRollupMetricNames(String... metricNames) {
    }

    @Override
    public void addRollupMetricName(String... metricNameParts) {
    }

    @Override
    public void addExclusiveRollupMetricName(String... metricNameParts) {
    }

    @Override
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
            String transactionSegmentUri) {
    }

    @Override
    public boolean isMetricProducer() {
        return false;
    }

    @Override
    public void setCustomMetricPrefix(String prefix) {
    }

    @Override
    public void setTrackChildThreads(boolean shouldTrack) {
    }

    @Override
    public boolean trackChildThreads() {
        return false;
    }

    @Override
    public void setTrackCallbackRunnable(boolean shouldTrack) {

    }

    @Override
    public boolean isTrackCallbackRunnable() {
        return false;
    }

    @Override
    public void excludeLeaf() {
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
    }

    @Override
    public void readInboundResponseHeaders(InboundHeaders inboundResponseHeaders) {
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
    }

    @Override
    public void reportAsExternal(com.newrelic.agent.bridge.external.ExternalParameters externalParameters) {
    }

}
