/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.google.common.collect.Queues;
import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.Token;
import com.newrelic.test.marker.RequiresFork;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Category(RequiresFork.class)
public class TransactionAsyncTimeoutTest implements ExtendedTransactionListener {

    private TransactionData data;
    private TransactionStats stats;
    private Queue<Transaction> runningTransactions;
    private int count;
    private List<TransactionStats> allStats = new ArrayList<>();

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        runningTransactions = null;
        count = 0;
        allStats.clear();
        TransactionAsyncUtility.createServiceManager(createConfigMap(1));
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    private static Map<String, Object> createConfigMap(int timeoutInSeconds) {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put("async_timeout", timeoutInSeconds);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    private void assertMetricCount(String metric, int count) {
        StatsImpl statsForMetric = (StatsImpl) stats.getUnscopedStats().getStats(metric);
        Assert.assertNotNull(statsForMetric);
        Assert.assertEquals(count, statsForMetric.getCallCount());
    }

    private void assertTokenMetricCounts(int create, int expire, int timeout, int linkIgnore, int linkSuccess) {
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_CREATE, create);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_EXPIRE, expire);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT, timeout);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_IGNORE, linkIgnore);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_SUCCESS, linkSuccess);
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
        if (runningTransactions == null) {
            runningTransactions = Queues.newConcurrentLinkedQueue();
        }

        runningTransactions.add(transaction);
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;
        count++;
        allStats.add(transactionStats);
        runningTransactions.remove(transactionData.getTransaction());
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
    }

    @Test
    public void testRegisterTwiceRunOnce() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        final Token t1 = tx.getToken();
        final Token t2 = tx.getToken();
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Thread a1 = new Thread() {
            @Override
            public void run() {
                Transaction.clearTransaction();
                Transaction oldTx = Transaction.getTransaction();
                Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one" + t1.toString());
                oldTx.getTransactionActivity().tracerStarted(rootTracer);

                t1.link();
                Transaction newTx = Transaction.getTransaction();
                Assert.assertTrue(oldTx != newTx);

                newTx.getTransactionActivity().tracerFinished(rootTracer, 0);
                t1.expire();
            }
        };
        a1.start();
        a1.join();

        Thread.sleep(1000);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        Thread.sleep(500);

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, tx.getFinishedChildren().size());
        assertTokenMetricCounts(2, 1, 1, 0, 1);
    }

    @Test
    public void testRegisterTwiceRunOnceLinkAndExpire() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        final Token t1 = tx.getToken();
        final Token t2 = tx.getToken();
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Thread a1 = new Thread() {
            @Override
            public void run() {
                Transaction.clearTransaction();
                Transaction oldTx = Transaction.getTransaction();
                Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one" + t1.toString());
                oldTx.getTransactionActivity().tracerStarted(rootTracer);

                boolean test = t1.linkAndExpire();
                Transaction newTx = Transaction.getTransaction();
                Assert.assertTrue(oldTx != newTx);

                newTx.getTransactionActivity().tracerFinished(rootTracer, 0);
            }
        };
        a1.start();
        a1.join();

        Thread.sleep(2000);
        ServiceFactory.getAsyncTxService().beforeHarvest("Unit Test", null);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        Thread.sleep(500);

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, tx.getFinishedChildren().size());
        assertTokenMetricCounts(2, 1, 1, 0, 1);
    }

    @Test
    public void testRegisterTwiceNoExpires() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        final Token t1 = tx.getToken();
        final Token t2 = tx.getToken();
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        Thread a1 = new Thread() {
            @Override
            public void run() {
                Transaction.clearTransaction();
                Transaction oldTx = Transaction.getTransaction();
                Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one" + t1.toString());
                oldTx.getTransactionActivity().tracerStarted(rootTracer);

                t1.link();
                Transaction newTx = Transaction.getTransaction();
                Assert.assertTrue(oldTx != newTx);

                Token t3 = newTx.getToken();
                newTx.getTransactionActivity().tracerFinished(rootTracer, 0);
                // no expire here
            }
        };
        a1.start();
        a1.join();

        Thread.sleep(1000);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        Thread.sleep(500);

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, tx.getFinishedChildren().size());
        assertTokenMetricCounts(3, 0, 3, 0, 1);
    }

    @Test
    public void testMarkLastTxaFinished() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        final Token token = tx.getToken();
        TransactionActivity txa = TransactionActivity.get();
        txa.tracerFinished(rootTracer, 0);
        Assert.assertEquals("Last Txa should have been marked finished.",
                txa.getRootTracer().getEndTime(),
                tx.getTransactionTimer().getTimeLastTxaFinished());

        Thread a1 = new Thread() {
            @Override
            public void run() {
                Transaction.clearTransaction();
                Transaction oldTx = Transaction.getTransaction();
                Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "thread" + token.toString());
                oldTx.getTransactionActivity().tracerStarted(rootTracer);

                token.link();
                Transaction newTx = Transaction.getTransaction();
                Assert.assertTrue(oldTx != newTx);

                TransactionActivity txa = TransactionActivity.get();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                txa.tracerFinished(rootTracer, 0);

                Assert.assertEquals("Last Txa should have been marked finished.",
                        txa.getRootTracer().getEndTime(),
                        newTx.getTransactionTimer().getTimeLastTxaFinished());
            }
        };
        a1.start();
        a1.join();
    }

}