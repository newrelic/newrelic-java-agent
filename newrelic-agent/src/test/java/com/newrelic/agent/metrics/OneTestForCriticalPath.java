/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionTrace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OneTestForCriticalPath implements TransactionListener {
    private List<TransactionData> dataList = new CopyOnWriteArrayList<>();
    private List<TransactionStats> statsList = new CopyOnWriteArrayList<>();

    private String fileName;

    public String getTestName() {
        return testName;
    }

    private String testName;
    private long expectedTxCount;
    private List<JsonTracer> tracers = new ArrayList<>();
    private final Multimap<String, JsonMetric> expectedScopedMetrics = ArrayListMultimap.create();
    private final List<JsonMetric> expectedUnscopedMetrics = new ArrayList<>();
    private final Map<String, AsyncComponent> threads = new HashMap<>();
    private final Map<Object, ContextObject> asyncContexts = new HashMap<>();
    private JsonTransactionEvent txEvent;
    private JsonTransactionTrace trace;

    public static OneTestForCriticalPath createOneTestForCriticalPath(JSONObject json, String pFileName) {

        OneTestForCriticalPath one = new OneTestForCriticalPath();
        one.fileName = pFileName;
        Object obj = json.get("testname");
        one.testName = (obj == null) ? null : (String) obj;

        obj = json.get("tracers");
        if (obj != null) {
            JSONArray traces = (JSONArray) obj;
            for (Object current : traces) {
                JsonTracer t = JsonTracer.createJsonTracer((JSONObject) current, one.asyncContexts);
                one.tracers.add(t);
            }
        }

        obj = json.get("scoped_metric_solution");
        if (obj != null) {
            JSONArray metrics = (JSONArray) obj;
            for (Object current : metrics) {
                JsonMetric metric = JsonMetric.createMetric((JSONArray) current, one.testName, one.fileName);
                one.expectedScopedMetrics.put(metric.getScope(), metric);
            }
        }
        obj = json.get("unscoped_metric_solution");
        if (obj != null) {
            JSONArray metrics = (JSONArray) obj;
            for (Object current : metrics) {
                JsonMetric metric = JsonMetric.createMetric((JSONArray) current, one.testName, one.fileName);
                one.expectedUnscopedMetrics.add(metric);
            }
        }
        one.expectedTxCount = one.expectedScopedMetrics.keySet().size();

        obj = json.get("transaction_trace");
        one.trace = (obj == null) ? null : JsonTransactionTrace.createJsonTransactionTrace((JSONObject) obj,
                one.testName, one.fileName);

        obj = json.get("transaction_event");
        one.txEvent = (obj == null) ? null : JsonTransactionEvent.createTransactionEvent((JSONObject) obj,
                one.testName, one.fileName);

        validate(one);

        return one;
    }

    private static void validate(OneTestForCriticalPath one) {
        if (one.testName == null) {
            throw new IllegalArgumentException("A test must have a testname.");
        } else if (one.tracers == null || one.tracers.size() == 0) {
            throw new IllegalArgumentException("There must be atleast one tracer");
        }
    }

    public void runTest() throws Exception {
        try {
            System.out.println("Running test " + testName);
            ServiceFactory.getTransactionService().addTransactionListener(this);
            long startTime = System.nanoTime();
            executeTest(startTime);
            validateResults();
        } finally {
            ServiceFactory.getTransactionService().removeTransactionListener(this);
        }
    }

    private void executeTest(long startTime) throws InterruptedException, ExecutionException {
        Queue<JsonTracer> toPull = new ConcurrentLinkedQueue<>(tracers);
        for (JsonTracer current : tracers) {
            AsyncComponent comp = threads.get(current.getAsyncUnit());
            if (comp == null) {
                comp = new AsyncComponent(current.getAsyncUnit(), toPull, startTime);
                threads.put(current.getAsyncUnit(), comp);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads.size());
        List<Future<Transaction>> outputs = new ArrayList<>(threads.size());
        for (AsyncComponent current : threads.values()) {
            outputs.add(executor.submit(current));
        }

        Transaction tx = null;
        for (Future<Transaction> currTx : outputs) {
            if (tx == null) {
                tx = currTx.get();
            } else {
                // Transaction newest = currTx.get();
                // Assert.assertEquals(newest, tx);
            }
        }

        Assert.assertNotNull(tx);

    }

    private void validateResults() {
        while (dataList.size() != expectedTxCount) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        Assert.assertEquals(expectedTxCount, dataList.size());
        Assert.assertEquals(expectedTxCount, statsList.size());

        verifyMetrics();
        verifyTransactionEvent();
        verifyTransactionTrace();
    }

    private void verifyTransactionTrace() {
        if (trace != null) {
            TransactionTrace actualTrace = TransactionTrace.getTransactionTrace(dataList.get(0));
            trace.validateTransactionTrace(actualTrace);
        }
    }

    private void verifyTransactionEvent() {
        if (txEvent != null) {
            txEvent.verifyTransactionEvent(dataList.get(0), statsList.get(0));
        }
    }

    private void verifyMetrics() {
        for (int i = 0; i < expectedTxCount; i++) {
            String scope = dataList.get(i).getBlameOrRootMetricName();
            SimpleStatsEngine scopedStats = statsList.get(i).getScopedStats();

            Map<String, StatsBase> actualMetrics = scopedStats.getStatsMap();
            Collection<JsonMetric> expectedM = expectedScopedMetrics.get(scope);
            Assert.assertNotNull(expectedM);
            Assert.assertEquals("Expected metric count did not match actual for scope " + scope, expectedM.size(),
                    actualMetrics.size());

            for (JsonMetric expCurr : expectedM) {
                StatsBase actCurr = actualMetrics.get(expCurr.getMetricName());
                Assert.assertNotNull("The following metric should be present but is not: " + expCurr.getMetricName(),
                        actCurr);
                expCurr.validateMetricExists(actCurr);
            }

            // verify unscoped metrics
            if (expectedUnscopedMetrics.size() > 0) {
                SimpleStatsEngine unscopedStats = statsList.get(i).getUnscopedStats();
                actualMetrics = unscopedStats.getStatsMap();

                for (JsonMetric expCurr : expectedUnscopedMetrics) {
                    StatsBase actCurr = actualMetrics.get(expCurr.getMetricName());
                    Assert.assertNotNull("The following metric should be present but is not: "
                            + expCurr.getMetricName(), actCurr);
                    expCurr.validateMetricExists(actCurr);
                }
            }
        }
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        dataList.add(transactionData);
        statsList.add(transactionStats);
    }
}
