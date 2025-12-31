/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.attributes.AttributeValidator;
import com.newrelic.agent.bridge.opentelemetry.SpanEvent;
import com.newrelic.agent.bridge.opentelemetry.SpanLink;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MockDispatcherTracer extends AbstractTracer implements Dispatcher, TransactionActivityInitiator {

    public MockDispatcherTracer() {
        super((TransactionActivity) null, new AttributeValidator(ATTRIBUTE_TYPE));
    }

    public MockDispatcherTracer(Transaction transaction) {
        super(new MockTransactionActivity(), new AttributeValidator(ATTRIBUTE_TYPE));
    }

    private long durationInMillis;
    private long startTime;
    private long endTime;
    private boolean isFinished;

    @Override
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getStartTimeInMilliseconds() {
        return 0;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public long getEndTimeInMilliseconds() {
        return 0;
    }

    @Override
    public long getExclusiveDuration() {
        return 0;
    }

    @Override
    public String getMetricName() {
        return null;
    }

    @Override
    public String getTransactionSegmentName() {
        return null;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return ImmutableMap.of();
    }

    @Override
    public void childTracerFinished(Tracer child) {
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public Tracer getParentTracer() {
        return null;
    }

    @Override
    public boolean isMetricProducer() {
        return false;
    }

    @Override
    public ClassMethodSignature getClassMethodSignature() {
        return null;
    }

    @Override
    public boolean isTransactionSegment() {
        return false;
    }

    @Override
    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
            long startTime, TransactionSegment lastSibling) {
        return null;
    }

    @Override
    public long getDurationInMilliseconds() {
        return durationInMillis;
    }

    public void setDurationInMilliseconds(long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    @Override
    public long getDuration() {
        return endTime - startTime;
    }

    @Override
    public long getRunningDurationInNanos() {
        return System.nanoTime() - startTime;
    }

    @Override
    public void finish(int opcode, Object returnValue) {
        isFinished = true;
    }

    @Override
    public void finish(Throwable throwable) {
    }

    @Override
    public void setTransactionName() {
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig() {
        return null;
    }

    @Override
    public boolean isWebTransaction() {
        return false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public boolean isParent() {
        return false;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public void setRequest(Request request) {
    }

    @Override
    public Response getResponse() {
        return null;
    }

    @Override
    public void setResponse(Response response) {
    }

    @Override
    public void setParentTracer(Tracer tracer) {
    }

    @Override
    public void setMetricName(String... metricNameParts) {
    }

    @Override
    public void addSpanLink(SpanLink link) {
    }

    @Override
    public List<SpanLink> getSpanLinks() {
        return Collections.emptyList();
    }

    @Override
    public void addSpanEvent(SpanEvent event) {
    }

    @Override
    public List<SpanEvent> getSpanEvents() {
        return Collections.emptyList();
    }

    @Override
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName, String transactionSegmentUri) {
    }

    @Override
    public void setIgnoreApdex(boolean ignore) {
    }

    @Override
    public Dispatcher createDispatcher() {
        return this;
    }

    @Override
    public boolean isIgnoreApdex() {
        return false;
    }

    @Override
    public void transactionFinished(String transactionName, TransactionStats stats) {
        isFinished = true;
    }

    @Override
    public void setAgentAttribute(String key, Object value) {
    }

    @Override
    public void removeAgentAttribute(String key) {

    }

    @Override
    public Object getAgentAttribute(String key) {
        return null;
    }

    @Override
    public void transactionActivityWithResponseFinished() {

    }

    @Override
    public void removeTransactionSegment() {

    }

    @Override
    public String getGuid() {
        return null;
    }
}
