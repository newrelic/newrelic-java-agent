/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.opentelemetry.SpanLink;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NoOpTracer implements Tracer {

    @Override
    public void setMetricName(String... metricNameParts) {
    }

    @Override
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
            String transactionSegmentUri) {
    }

    @Override
    public void addSpanLink(SpanLink link) {
    }

    @Override
    public List<SpanLink> getSpanLinks() {
        return Collections.emptyList();
    }

    @Override
    public TracedMethod getParentTracedMethod() {
        return null;
    }

    @Override
    public void finish(Throwable throwable) {
    }

    @Override
    public void finish(int opcode, Object returnValue) {
    }

    @Override
    public long getDurationInMilliseconds() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    @Override
    public boolean isTransactionSegment() {
        return false;
    }

    @Override
    public boolean isParent() {
        return false;
    }

    @Override
    public void setParentTracer(Tracer tracer) {
    }

    @Override
    public boolean isMetricProducer() {
        return true;
    }

    @Override
    public boolean isChildHasStackTrace() {
        return false;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }

    @Override
    public String getTransactionSegmentName() {
        return null;
    }

    @Override
    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
            long startTime, TransactionSegment lastSibling) {
        return null;
    }

    @Override
    public long getStartTimeInMilliseconds() {
        return 0;
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @Override
    public long getRunningDurationInNanos() {
        return 0;
    }

    @Override
    public Tracer getParentTracer() {
        return null;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        return Collections.emptyMap();
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
        return null;
    }

    @Override
    public long getExclusiveDuration() {
        return 0;
    }

    @Override
    public long getEndTimeInMilliseconds() {
        return 0;
    }

    @Override
    public long getEndTime() {
        return 0;
    }

    @Override
    public ClassMethodSignature getClassMethodSignature() {
        return null;
    }

    @Override
    public void childTracerFinished(Tracer child) {
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void setRollupMetricNames(String... metricNames) {
    }

    @Override
    public void nameTransaction(com.newrelic.agent.bridge.TransactionNamePriority namePriority) {
    }

    @Override
    public void addRollupMetricName(String... metricNameParts) {
    }

    @Override
    public void addExclusiveRollupMetricName(String... metricNameParts) {
    }

    @Override
    public void setAgentAttribute(String key, Object value) {
    }

    @Override
    public void setAgentAttribute(String key, Object value, boolean addToSpan) {

    }

    @Override
    public void removeAgentAttribute(String key) {
    }

    @Override
    public Object getAgentAttribute(String key) {
        return null;
    }

    @Override
    public TransactionActivity getTransactionActivity() {
        return null;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void setCustomMetricPrefix(String prefix) {
    }

    @Override
    public void removeTransactionSegment() {
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

    @Override
    public void markFinishTime() {
    }

    @Override
    public String getGuid() {
        return null;
    }

    @Override
    public long getStartTimeInMillis() {
        return 0;
    }

    @Override
    public ExternalParameters getExternalParameters() {
        return null;
    }

    @Override
    public Set<String> getAgentAttributeNamesForSpans() {
        return null;
    }

    @Override
    public void setNoticedError(Throwable throwable) {
    }

    @Override
    public Throwable getException() {
        return null;
    }

    @Override
    public void setThrownException(Throwable throwable) {
    }

    @Override
    public boolean wasExceptionSetByAPI() {
        return false;
    }
}
