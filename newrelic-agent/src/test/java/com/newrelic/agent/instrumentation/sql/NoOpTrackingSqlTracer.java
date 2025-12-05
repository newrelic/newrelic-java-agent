/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.sql;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.datastore.ConnectionFactory;
import com.newrelic.agent.bridge.opentelemetry.SpanLink;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.ExplainPlanExecutor;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoOpTrackingSqlTracer implements SqlTracer {
    public Connection connection = null;
    public ConnectionFactory connectionFactory = null;
    public String rawSql = null;
    public Object[] params = null;

    public Transaction tx = null;
    public Object sql = null;

    public NoOpTrackingSqlTracer() {
    }

    public NoOpTrackingSqlTracer(Transaction tx, Object sql) {
        this.tx = tx;
        this.sql = sql;
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Override
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public String getRawSql() {
        return rawSql;
    }

    @Override
    public void provideConnection(Connection conn) {
        this.connection = conn;
    }

    @Override
    public void setRawSql(String rawSql) {
        this.rawSql = rawSql;
    }

    @Override
    public Object[] getParams() {
        return params;
    }

    @Override
    public void setParams(Object[] params) {
        this.params = params;
    }

    @Override
    public boolean hasExplainPlan() {
        return false;
    }

    @Override
    public ExplainPlanExecutor getExplainPlanExecutor() {
        return null;
    }

    @Override
    public Object getSql() {
        return sql;
    }

    @Override
    public void setExplainPlan(Object... explainPlan) {

    }

    @Override
    public Transaction getTransaction() {
        return tx;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public Integer getPort() {
        return null;
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @Override
    public long getStartTimeInMilliseconds() {
        return 0;
    }

    @Override
    public long getEndTime() {
        return 0;
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
    public long getRunningDurationInNanos() {
        return 0;
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
    public String getTransactionSegmentName() {
        return null;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public void setAgentAttribute(String key, Object value) {

    }

    @Override
    public void setAgentAttribute(String key, Object value, boolean addToSpan) {

    }

    @Override
    public Object getAgentAttribute(String key) {
        return null;
    }

    @Override
    public void childTracerFinished(Tracer child) {

    }

    @Override
    public Tracer getParentTracer() {
        return null;
    }

    @Override
    public void setParentTracer(Tracer tracer) {

    }

    @Override
    public boolean isParent() {
        return false;
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
    public boolean isChildHasStackTrace() {
        return false;
    }

    @Override
    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
            long startTime, TransactionSegment lastSibling) {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void finish(int opcode, Object returnValue) {

    }

    @Override
    public void finish(Throwable throwable) {

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
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
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
            String transactionSegmentUri) {

    }

    @Override
    public void addExclusiveRollupMetricName(String... metricNameParts) {

    }

    @Override
    public void nameTransaction(TransactionNamePriority namePriority) {

    }

    @Override
    public void setMetricName(String... metricNameParts) {

    }

    @Override
    public void addRollupMetricName(String... metricNameParts) {

    }

    @Override
    public TransactionActivity getTransactionActivity() {
        return tx.getTransactionActivity();
    }

    @Override
    public void removeAgentAttribute(String key) {

    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void removeTransactionSegment() {

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
    public void setThrownException(Throwable throwable) {
    }

    @Override
    public boolean wasExceptionSetByAPI() {
        return false;
    }

    @Override
    public Throwable getException() {
        return null;
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
    public int getChildCount() {
        return 0;
    }

    @Override
    public void excludeLeaf() {
    }
}
