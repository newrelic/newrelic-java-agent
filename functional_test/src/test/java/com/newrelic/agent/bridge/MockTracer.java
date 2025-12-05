/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.bridge.opentelemetry.SpanLink;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

public class MockTracer implements ExitTracer {

    private String[] metricName;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    @Override
    public void setMetricName(String... metricNameParts) {
        this.metricName = metricNameParts;
    }

    @Override
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName, String transactionSegmentUri) {
        this.metricName = new String[] { metricName };
    }

    @Override
    public void finish(int opcode, Object returnValue) {
    }

    @Override
    public void finish(Throwable throwable) {
    }

    public String[] getMetricNames() {
        return metricName;
    }

    @Override
    public void nameTransaction(TransactionNamePriority priority) {
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

    public void addCustomAttributes(Map<String, Object> attributes) {
    }

    @Override
    public String getMetricName() {
        return Strings.join('/', metricName);
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
    public void setRollupMetricNames(String... metricNames) {
    }

    @Override
    public void addRollupMetricName(String... metricNameParts) {
    }

    @Override
    public void addExclusiveRollupMetricName(String... metricNameParts) {
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
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
    }

    public void readInboundResponseHeaders(InboundHeaders inboundResponseHeaders) {
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
    }

    @Override
    public void reportAsExternal(com.newrelic.agent.bridge.external.ExternalParameters externalParameters) {
    }

    @Override
    public void excludeLeaf(){
    }
}
