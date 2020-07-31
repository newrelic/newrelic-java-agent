/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class SlowQueryAggregatorImpl implements SlowQueryAggregator {

    public static final String BACKTRACE_KEY = "backtrace";
    public static final String EXPLAIN_PLAN_KEY = "explain_plan";
    public static final int SLOW_QUERY_LIMIT_PER_REPORTING_PERIOD = 10;

    /**
     * The maximum number of slow query traces we allow in the collection. Package visibility for tests.
     */
    static final int MAX_SLOW_QUERY_STATEMENTS = 200;

    private final BoundedConcurrentCache<String, SlowQueryInfo> slowQueries =
            new BoundedConcurrentCache<>(MAX_SLOW_QUERY_STATEMENTS);
    private final Lock readLock;
    private final Lock writeLock;

    public SlowQueryAggregatorImpl() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    @Override
    public List<SqlTrace> getAndClearSlowQueries() {
        List<SlowQueryInfo> slowQueries = getAndClearSlowQueriesUnderLock();

        if (slowQueries == null || slowQueries.isEmpty()) {
            return Collections.emptyList();
        }
        return createSqlTraces(slowQueries);
    }

    // For use by tests. Package visibility. Intentionally violates locking
    // protocol. Not the same as calling getAndClearSlowQueries().size().
    public int getSlowQueryCount() {
        return slowQueries.size();
    }

    // Only use this for testing.
    protected List<SlowQueryInfo> getSlowQueriesForTesting() {
        return slowQueries.asList();
    }

    private List<SqlTrace> createSqlTraces(List<SlowQueryInfo> slowQueries) {
        List<SlowQueryInfo> topSlowQueries = getTopSlowQueries(slowQueries);
        List<SqlTrace> results = new ArrayList<>(topSlowQueries.size());
        for (SlowQueryInfo slowQuery : topSlowQueries) {
            // TransactionData is required in order to send up a slow query trace
            if (slowQuery.getTransactionData() != null) {
                results.add(slowQuery.asSqlTrace());
            }
        }
        return results;
    }

    private List<SlowQueryInfo> getTopSlowQueries(List<SlowQueryInfo> slowQueries) {
        if (slowQueries.size() <= SLOW_QUERY_LIMIT_PER_REPORTING_PERIOD) {
            return slowQueries;
        }
        Collections.sort(slowQueries);
        return slowQueries.subList(slowQueries.size() - SLOW_QUERY_LIMIT_PER_REPORTING_PERIOD, slowQueries.size());
    }

    private List<SlowQueryInfo> getAndClearSlowQueriesUnderLock() {
        writeLock.lock();
        try {
            List<SlowQueryInfo> results = slowQueries.asList();
            slowQueries.clear();
            return results;
        } finally {
            writeLock.unlock();
        }

    }

    @Override
    public void addSlowQueriesFromTransaction(TransactionData td) {
        SlowQueryListener listener = td.getSlowQueryListener();
        if (listener == null) {
            Agent.LOG.finest("SlowQueryAggregator: addSlowQueriesFromTransaction: no listener");
            return;
        }
        List<SlowQueryInfo> slowQueries = listener.getSlowQueries();
        if (slowQueries.isEmpty()) {
            Agent.LOG.finest("SlowQueryAggregator: addSlowQueriesFromTransaction: no slow queries");
            return;
        }

        Agent.LOG.log(Level.FINEST, "SlowQueryAggregator: addSlowQueriesFromTransaction: slow queries: {0}",
                slowQueries.size());
        addSlowQueriesUnderLock(td, slowQueries);
    }

    // Protected for testing purposes. Use addSlowQueriesFromTransaction instead of this method for all app code.
    protected void addSlowQueriesUnderLock(TransactionData td, List<SlowQueryInfo> slowQueries) {
        readLock.lock();
        try {
            for (SlowQueryInfo slowQuery : slowQueries) {
                addSlowQuery(td, slowQuery);
            }
        } finally {
            readLock.unlock();
        }
    }

    private void addSlowQuery(TransactionData td, SlowQueryInfo slowQuery) {
        String obfuscatedQuery = slowQuery.getObfuscatedQuery();

        // always set the transaction data on the slow query
        if (slowQuery.getTransactionData() == null) {
            // Now that we have the transaction data, add it to the slow query object.
            slowQuery.setTransactionData(td);
        }

        SlowQueryInfo existingInfo = slowQueries.get(obfuscatedQuery);
        if (existingInfo != null) {
            existingInfo.aggregate(slowQuery);
            slowQueries.putReplace(obfuscatedQuery, existingInfo);
        } else {
            slowQueries.putIfAbsent(obfuscatedQuery, slowQuery);
        }
    }

}
