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
import com.newrelic.agent.TokenImpl;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.test.marker.RequiresFork;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.objectweb.asm.Opcodes;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Async timeout is given in seconds. Sleep values need to be at least as long as the value passed into createConfigMap,
 * where 0s defaults to 250ms and 1s is 1000ms.
 */
@Category(RequiresFork.class)
public class TokenTimeoutTest implements ExtendedTransactionListener {

    private List<TransactionData> data;
    private List<TransactionStats> stats;

    private Queue<Transaction> runningTransactions;

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        runningTransactions = null;
        TransactionAsyncUtility.createServiceManager(createConfigMap(0));
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    private static Map<String, Object> createConfigMap(int timeoutInSeconds) {
        Map<String, Object> map = new HashMap<>();
        map.put("token_timeout", timeoutInSeconds);
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    private void assertMetricCount(String metric, int count) {
        StatsImpl statsForMetric = (StatsImpl) stats.get(0).getUnscopedStats().getStats(metric);
        Assert.assertNotNull(statsForMetric);
        assertEquals(count, statsForMetric.getCallCount());
    }

    private void assertTokenMetricCounts(int create, int expire, int timeout, int linkIgnore, int linkSuccess) {
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_CREATE, create);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_EXPIRE, expire);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT, timeout);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_IGNORE, linkIgnore);
        assertMetricCount(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_SUCCESS, linkSuccess);
    }

    @Test
    public void testOneTokenNotExpired() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        tx.setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH, false, "CustomCategory",
                "names");

        TransactionAsyncUtility.StartAndThenLink activity1 = new TransactionAsyncUtility.StartAndThenLink(tx, false,
                false);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        busyWait(250);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        busyWait(500);

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        assertEquals(1, stats.size());
        assertTokenMetricCounts(1, 0, 1, 0, 1);
        assertEquals("RequestDispatcher", rootTracer.getMetricName());
        assertEquals("Truncated/RequestDispatcher", rootTracer.getTransactionSegmentName());

        assertEquals(TimeoutCause.TOKEN, tx.getTimeoutCause());

        String cause = MessageFormat.format(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT_CAUSE,
                "com.newrelic.agent.transaction.TokenTimeoutTest.hi()V");
        assertEquals(1, ServiceFactory.getStatsService().getStatsEngineForHarvest("Unit Test").getStats(
                cause).getCallCount());
    }

    @Test
    public void testExpireAfterAccess() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        tx.setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH, false, "CustomCategory",
                "names");

        Assert.assertNotNull(token.getTransaction());
        Assert.assertNotNull(token.getInitiatingTracer());

        // if refresh didn't work this token would expire after 250ms

        busyWait(200);
        tx.refreshToken(token);
        ServiceFactory.getTransactionService().processQueue();

        Assert.assertNotNull(token.getTransaction());
        Assert.assertNotNull(token.getInitiatingTracer());

        busyWait(200);
        tx.refreshToken(token);
        ServiceFactory.getTransactionService().processQueue();

        busyWait(300);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        busyWait(500);

        Assert.assertNull(token.getInitiatingTracer());
        Assert.assertNull(token.getInitiatingTracer());

        String cause = MessageFormat.format(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT_CAUSE,
                "com.newrelic.agent.transaction.TokenTimeoutTest.hi()V");
        assertEquals(1, ServiceFactory.getStatsService().getStatsEngineForHarvest("Unit Test").getStats(
                cause).getCallCount());
    }

    @Test
    public void testNoExpirationWhileRunning() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Tracer tokenInitTracer = token.getInitiatingTracer();

        tx.setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH, false, "CustomCategory",
                "names");

        rootTracer.finish(Opcodes.RETURN, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        busyWait(250);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        busyWait(500);

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        assertEquals(1, stats.size());
        assertTokenMetricCounts(1, 0, 1, 0, 0);
        assertEquals("RequestDispatcher", tokenInitTracer.getMetricName());
        assertEquals("Truncated/RequestDispatcher", tokenInitTracer.getTransactionSegmentName());
        Assert.assertNull(tx.getAgentAttributes().get(AttributeNames.THREAD_NAME));

        assertEquals(TimeoutCause.TOKEN, tx.getTimeoutCause());

        String cause = MessageFormat.format(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT_CAUSE,
                "com.newrelic.agent.transaction.TokenTimeoutTest.hi()V");
        assertEquals(1, ServiceFactory.getStatsService().getStatsEngineForHarvest("Unit Test").getStats(
                cause).getCallCount());
    }

    @Test
    public void testLotsOfTokensNotExpired() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token3 = (TokenImpl) tx.getToken();
        TokenImpl token4 = (TokenImpl) tx.getToken();

        Tracer token3InitTracer = token3.getInitiatingTracer();

        tx.setTransactionName(com.newrelic.api.agent.TransactionNamePriority.CUSTOM_HIGH, false, "CustomCategory",
                "names");

        // expire after root tracer finish
        TransactionAsyncUtility.StartAndThenLink activity1 = new TransactionAsyncUtility.StartAndThenLink(tx, false,
                true);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        // expire before root finish
        TransactionAsyncUtility.StartAndThenLink activity2 = new TransactionAsyncUtility.StartAndThenLink(tx, true,
                false);
        activity2.start();
        activity2.join();

        // if the threads above take longer than 250ms to start and finish then these null checks will cause a flicker
        Assert.assertNull(data);
        Assert.assertNull(stats);

        busyWait(250);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        busyWait(500);

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        assertEquals(1, stats.size());
        assertTokenMetricCounts(4, 2, 2, 0, 2);

        assertEquals("RequestDispatcher", token3InitTracer.getMetricName());
        assertEquals("Truncated/RequestDispatcher", token3InitTracer.getTransactionSegmentName());

        String cause = MessageFormat.format(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT_CAUSE,
                "com.newrelic.agent.transaction.TokenTimeoutTest.hi()V");
        assertEquals(2, ServiceFactory.getStatsService().getStatsEngineForHarvest("Unit Test").getStats(
                cause).getCallCount());
    }

    @Test
    public void testMultipleTransactions() throws InterruptedException {
        List<BasicTransaction> btxs = new ArrayList<>();
        btxs.add(new BasicTransaction(1));
        btxs.add(new BasicTransaction(2));
        btxs.add(new BasicTransaction(3));
        btxs.add(new BasicTransaction(4));
        btxs.add(new BasicTransaction(5));

        for (BasicTransaction current : btxs) {
            current.start();
        }
        for (BasicTransaction current : btxs) {
            current.join();
        }

        Assert.assertNull(data);
        Assert.assertNull(stats);

        busyWait(250);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        busyWait(2000);

        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        assertEquals(5, stats.size());
    }

    @Test
    public void testTxnAttrTokenTimeout() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);

        // Name the transaction so we can identify it in the listener below and create an event
        tx.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "TokenTimeout", "timeout");

        final List<TransactionEvent> events = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        ServiceFactory.getServiceManager().getTransactionService().addTransactionListener(new TransactionListener() {
            @Override
            public void dispatcherTransactionFinished(TransactionData transactionData,
                    TransactionStats transactionStats) {
                if (transactionData.getPriorityTransactionName().getName().equals(
                        "WebTransaction/TokenTimeout/timeout")) {
                    events.add(ServiceFactory.getTransactionEventsService().createEvent(transactionData, transactionStats,
                            transactionData.getBlameOrRootMetricName()));
                    latch.countDown();
                }
            }
        });

        tx.getTransactionActivity().tracerStarted(rootTracer);

        // Let this timeout the transaction
        Token token = tx.getToken();

        rootTracer.finish(Opcodes.RETURN, 0);
        assertFalse(tx.isFinished());

        // Don't start the thread. The timeout is configured to 0 seconds.
        // Allow it to expire and then run the code that implements it.

        busyWait(1000);
        latch.await();

        assertTrue(tx.isFinished());
        assertFalse(events.isEmpty());
        assertEquals("WebTransaction/TokenTimeout/timeout", events.get(0).getName());
        assertEquals(TimeoutCause.TOKEN, events.get(0).getTimeoutCause());
    }

    /**
     * The transaction is linked prior to the start of work.
     */
    public static class BasicTransaction extends Thread {

        private int value;

        public BasicTransaction(int input) {
            value = input;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                TransactionActivity.clear();
                Tracer rootTracer = TransactionAsyncUtility.createOtherTracer("root" + value);
                Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);
                Transaction.getTransaction().getToken();
                rootTracer.finish(Opcodes.RETURN, 0);
            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
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
        if (data == null) {
            data = new CopyOnWriteArrayList<>();
        }
        if (stats == null) {
            stats = new CopyOnWriteArrayList<>();
        }

        data.add(transactionData);
        stats.add(transactionStats);

        runningTransactions.remove(transactionData.getTransaction());
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
    }

    private void busyWait(long waitTimeInMillis) {
        long endTimeInNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitTimeInMillis);
        while (System.nanoTime() < endTimeInNanos) {
        }
    }

}
