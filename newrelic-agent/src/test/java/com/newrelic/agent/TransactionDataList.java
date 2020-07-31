/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.TransactionStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TransactionDataList extends CopyOnWriteArrayList<TransactionData> implements TransactionListener {

    private final StatsEngine statsEngine = new StatsEngineImpl();

    private static final long serialVersionUID = 1787029471978321417L;

    @Override
    public void clear() {
        super.clear();
        statsEngine.clear();
    }

    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        add(transactionData);

        Agent.LOG.info("Transaction: " + transactionData.getBlameMetricName() + ", Metrics: " + transactionStats);
        synchronized (statsEngine) {
            try {
                statsEngine.mergeStatsResolvingScope(clone(transactionStats), transactionData.getBlameMetricName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private TransactionStats clone(TransactionStats transactionStats) throws CloneNotSupportedException {
        TransactionStats stats = new TransactionStats();
        clone(transactionStats.getScopedStats(), stats.getScopedStats());
        clone(transactionStats.getUnscopedStats(), stats.getUnscopedStats());
        return stats;
    }

    private void clone(SimpleStatsEngine from, SimpleStatsEngine to) throws CloneNotSupportedException {
        for (Entry<String, StatsBase> entry : from.getStatsMap().entrySet()) {
            StatsBase statsBase = to.getStatsMap().get(entry.getKey());
            if (statsBase == null) {
                to.getStatsMap().put(entry.getKey(), (StatsBase) entry.getValue().clone());
            } else {
                to.getStats(entry.getKey()).merge(entry.getValue());
            }
        }
    }

    public static List<TransactionData> getTransactions(Runnable runnable) {
        TransactionDataList list = new TransactionDataList();
        TransactionService service = ServiceFactory.getTransactionService();
        service.addTransactionListener(list);

        runnable.run();

        service.removeTransactionListener(list);
        return list;
    }

    public List<MetricName> getMetricNames() {
        synchronized (statsEngine) {
            return statsEngine.getMetricNames();
        }

    }

    public Map<String, Integer> getMetricCounts(Set<MetricName> responseTimeMetricNames) {
        Map<String, Integer> metricNameToCounts = new HashMap<>();

        synchronized (statsEngine) {
            for (MetricName responseTimeMetricName : responseTimeMetricNames) {
                ResponseTimeStats responseTimeStats = statsEngine.getResponseTimeStats(responseTimeMetricName);
                metricNameToCounts.put(responseTimeMetricName.getName(), responseTimeStats.getCallCount());
            }
        }

        return metricNameToCounts;
    }

    public Set<String> getMetricNameStrings() {
        return new HashSet<>(Lists.transform(getMetricNames(), new Function<MetricName, String>() {
            public String apply(MetricName name) {
                return name.getName();
            }
        }));
    }

    public List<String> getTransactionNames() {
        List<String> txNames = new ArrayList<>();
        for (TransactionData data : this) {
            txNames.add(data.getBlameMetricName());
        }
        return txNames;
    }

    public TransactionDataList waitFor(int transactionCount, long timeoutInMillis) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutInMillis;
        while(size() < transactionCount  && System.currentTimeMillis() < endTime) {
            Thread.sleep(10);
        }
        return this;
    }
}
