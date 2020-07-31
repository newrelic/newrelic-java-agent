/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.DatastoreConfig;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.SqlTraceConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.SqlTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.util.StackTraces;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SlowQueryInfo implements Comparable<SlowQueryInfo>, CacheValue<String> {

    private final AtomicReference<SlowQueryTracerInfo> slowestQuery = new AtomicReference<>();

    private final long id;
    private final AtomicInteger callCount = new AtomicInteger();
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong max = new AtomicLong();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

    private final String rawQuery;
    private final String obfuscatedQuery;

    SlowQueryInfo(TransactionData td, Tracer tracer, String rawQuery, String obfuscatedQuery, SqlTraceConfig sqlTraceConfig) {
        this.slowestQuery.set(new SlowQueryTracerInfo(td, tracer));
        this.rawQuery = rawQuery;
        this.obfuscatedQuery = obfuscatedQuery;
        this.id = generateId(sqlTraceConfig, obfuscatedQuery);
    }

    private long generateId(SqlTraceConfig sqlTraceConfig, String obfuscatedQuery) {
        long obfuscatedHash = (long) obfuscatedQuery.hashCode();
        if (sqlTraceConfig.isUsingLongerSqlId()) {
            obfuscatedHash = createLongerHashCode(obfuscatedHash);
        }
        return obfuscatedHash;
    }

    protected static long createLongerHashCode(long hashedQuery) {
        String hashString = String.valueOf(hashedQuery);
        long positiveHash = hashedQuery < 0 ? (hashedQuery * -1) : hashedQuery;
        String positiveHashString = String.valueOf(positiveHash);

        if (positiveHashString.length() == 9) {
            hashString += positiveHashString.charAt(0);
        }

        return Long.parseLong(hashString);
    }

    public TransactionData getTransactionData() {
        return slowestQuery.get().getTransactionData();
    }

    public Tracer getTracer() {
        return slowestQuery.get().getTracer();
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public String getObfuscatedQuery() {
        return obfuscatedQuery;
    }

    @Override
    public int compareTo(SlowQueryInfo other) {
        Long thisMax = max.get();
        Long otherMax = other.max.get();
        int compare = thisMax.compareTo(otherMax);
        if (compare == 0) {
            Long thisTotal = total.get();
            Long otherTotal = other.total.get();
            return thisTotal.compareTo(otherTotal);
        }
        return compare;
    }

    @Override
    public String getKey() {
        return obfuscatedQuery;
    }

    public void aggregate(Tracer tracer) {
        this.aggregate(null, tracer);
    }

    public void aggregate(TransactionData td, Tracer tracer) {
        callCount.incrementAndGet();
        long duration = tracer.getDuration();
        total.addAndGet(duration);
        replaceMin(duration);
        replaceMax(duration);
        replaceTracer(td, tracer);
    }

    public void aggregate(SlowQueryInfo other) {
        long duration = other.getTracer().getDuration();
        total.addAndGet(other.getTotalInNano());
        callCount.addAndGet(other.getCallCount());
        replaceMin(duration);
        replaceMax(duration);
        replaceTracer(other.getTransactionData(), other.getTracer());
    }

    public SqlTrace asSqlTrace() {
        Tracer tracer = getTracer();
        if (tracer instanceof SqlTracer) {
            DatabaseService dbService = ServiceFactory.getDatabaseService();
            dbService.runExplainPlan((SqlTracer) tracer);
        }
        return new SqlTraceImpl(this);
    }

    public String getBlameMetricName() {
        return getTransactionData().getBlameMetricName();
    }

    public String getMetricName() {
        return getTracer().getMetricName();
    }

    public long getId() {
        return id;
    }

    public String getQuery() {
        String query = obfuscatedQuery;

        // TransactionData can be null but by the time we get here it will already be checked for null and excluded
        TransactionTracerConfig ttConfig = getTransactionData().getTransactionTracerConfig();
        RecordSql recordSql = RecordSql.get(ttConfig.getRecordSql());
        if (recordSql == RecordSql.raw) {
            query = rawQuery;
        }

        return TransactionSegment.truncateSql(query, ttConfig.getInsertSqlMaxLength());
    }

    public String getRequestUri() {
        // No slow query attribute configuration. Use root level configuration.
        return getTransactionData().getRequestUri(AgentConfigImpl.ATTRIBUTES);
    }

    public int getCallCount() {
        return callCount.get();
    }

    public long getTotalInNano() {
        return total.get();
    }

    public long getTotalInMillis() {
        return TimeUnit.MILLISECONDS.convert(total.get(), TimeUnit.NANOSECONDS);
    }

    public long getMinInMillis() {
        return TimeUnit.MILLISECONDS.convert(min.get(), TimeUnit.NANOSECONDS);
    }

    public long getMaxInMillis() {
        return TimeUnit.MILLISECONDS.convert(max.get(), TimeUnit.NANOSECONDS);
    }

    public Map<String, Object> getParameters() {
        return createParameters(getTracer());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createParameters(Tracer tracer) {
        Map<String, Object> parameters = new HashMap<>();

        // Check for an explain plan (right now this only potentially exists for a SqlTracer)
        Object explainPlan = tracer.getAgentAttribute(SqlTracer.EXPLAIN_PLAN_PARAMETER_NAME);
        if (explainPlan != null) {
            parameters.put(SlowQueryAggregatorImpl.EXPLAIN_PLAN_KEY, explainPlan);
        }

        // A backtrace could exist for any type of tracer
        List<StackTraceElement> backtrace = (List<StackTraceElement>) tracer.getAgentAttribute(
                DefaultTracer.BACKTRACE_PARAMETER_NAME);
        if (backtrace != null) {
            backtrace = StackTraces.scrubAndTruncate(backtrace);
            List<String> backtraceStrings = StackTraces.toStringList(backtrace);
            parameters.put(SlowQueryAggregatorImpl.BACKTRACE_KEY, backtraceStrings);
        }

        DatastoreConfig datastoreConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDatastoreConfig();

        String host = (String) tracer.getAgentAttribute(DatastoreMetrics.DATASTORE_HOST);
        String port_path_or_id = (String) tracer.getAgentAttribute(DatastoreMetrics.DATASTORE_PORT_PATH_OR_ID);
        boolean allUnknown = host == null && port_path_or_id == null;
        if (datastoreConfig.isInstanceReportingEnabled() && !allUnknown) {
            parameters.put(DatastoreMetrics.DATASTORE_HOST, host);
            parameters.put(DatastoreMetrics.DATASTORE_PORT_PATH_OR_ID, port_path_or_id);
        }

        String databaseName = (String) tracer.getAgentAttribute(DatastoreMetrics.DB_INSTANCE);
        if (datastoreConfig.isDatabaseNameReportingEnabled() && databaseName != null) {
            parameters.put(DatastoreMetrics.DB_INSTANCE, databaseName);
        }

        // An input query could exist for any type of tracer and records ORM-like query strings
        Map<String, String> inputQuery = (Map<String, String>) tracer.getAgentAttribute(DatastoreMetrics.INPUT_QUERY_ATTRIBUTE);
        if (inputQuery != null) {
            parameters.put(DatastoreMetrics.INPUT_QUERY_ATTRIBUTE, inputQuery);
        }

        Transaction txn = tracer.getTransactionActivity().getTransaction();
        DistributedTracePayloadImpl inboundPayload = txn.getSpanProxy().getInboundDistributedTracePayload();

        DistributedTraceService distributedTraceService = ServiceFactory.getDistributedTraceService();
        DistributedTracingConfig distributedTracingConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig();

        if (distributedTracingConfig.isEnabled()) {
            String traceId = txn.getOrCreateTraceId();
            String parentId = inboundPayload == null ? null : inboundPayload.txnId;
            String parentSpanId = inboundPayload == null ? null : inboundPayload.guid;
            Map<String, Object> intrinsics = distributedTraceService.getIntrinsics(inboundPayload, txn.getGuid(), traceId,
                    txn.getTransportType(), txn.getTransportDurationInMillis(), txn.getLargestTransportDurationInMillis(),
                    parentId, parentSpanId, txn.getPriority());
            parameters.putAll(intrinsics);
        }

        parameters.put("priority", txn.getPriority());

        return parameters;
    }

    private void replaceMin(long duration) {
        while (true) {
            long currentDuration = min.get();
            if (duration >= currentDuration) {
                return;
            }
            if (min.compareAndSet(currentDuration, duration)) {
                return;
            }
        }
    }

    private void replaceMax(long duration) {
        while (true) {
            long currentDuration = max.get();
            if (duration <= currentDuration) {
                return;
            }
            if (max.compareAndSet(currentDuration, duration)) {
                return;
            }
        }
    }

    private void replaceTracer(TransactionData td, Tracer tracer) {
        while (true) {
            SlowQueryTracerInfo current = slowestQuery.get();
            if (tracer.getDuration() <= current.getTracer().getDuration()) {
                return;
            }
            SlowQueryTracerInfo update = new SlowQueryTracerInfo(td, tracer);
            if (slowestQuery.compareAndSet(current, update)) {
                return;
            }
        }
    }

    public void setTransactionData(TransactionData td) {
        slowestQuery.get().setTransactionData(td);
    }

}
