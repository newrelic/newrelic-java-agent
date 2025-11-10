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
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.test.marker.RequiresFork;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static org.junit.Assert.*;

/**
 * Async timeout is given in seconds. Sleep values need to be at least as long as the value passed into createConfigMap,
 * where 0s defaults to 250ms and 1s is 1000ms.
 */
@Category(RequiresFork.class)
public class AsyncTimeoutTransactionTest implements ExtendedTransactionListener {

    private TransactionData data;
    private TransactionStats stats;
    private Queue<Transaction> runningTransactions;
    private AsyncTransactionService asyncService;
    private TransactionService txService;

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        runningTransactions = null;
        TransactionAsyncUtility.createServiceManager(createConfigMap(1)); // 1000ms
        ServiceFactory.getTransactionService().addTransactionListener(this);
        asyncService = ServiceFactory.getAsyncTxService();
        txService = ServiceFactory.getTransactionService();
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    private static Map<String, Object> createConfigMap(int timeoutInSeconds) {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put("token_timeout", timeoutInSeconds);
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    private void assertTokenMetricCounts(int create, int expire, int timeout, int linkIgnore, int linkSuccess) {
        assertEquals(create,
                stats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_CREATE).getCallCount());
        assertEquals(expire,
                stats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_EXPIRE).getCallCount());
        assertEquals(timeout,
                stats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT).getCallCount());
        assertEquals(linkIgnore,
                stats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_IGNORE).getCallCount());
        assertEquals(linkSuccess,
                stats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_LINK_SUCCESS).getCallCount());
    }

    @Test
    public void testRegular() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(rootTracer);

        assertNull(data);
        assertNull(stats);

        tx.getTransactionActivity().tracerFinished(rootTracer, 0);
        ServiceFactory.getTransactionService().processQueue();

        assertNotNull(data);
        assertNotNull(stats);

        assertEquals(1, tx.getFinishedChildren().size());
        assertTokenMetricCounts(0, 0, 0, 0, 0);
    }

    @Test
    public void testTimeout() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(rootTracer);

        asyncService.registerAsyncActivity("123");
        asyncService.registerAsyncActivity(new Object());

        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        assertNull(data);
        assertNull(stats);

        Thread.sleep(1000);
        ServiceFactory.getTransactionService().processQueue();

        // forces timeout to occur if time has passed
        asyncService.beforeHarvest("test", null);
        // wait for async token timeouts to complete
        Thread.sleep(500);

        assertNotNull(data);
        assertNotNull(stats);

        assertEquals(1, tx.getFinishedChildren().size());
        assertTokenMetricCounts(2, 0, 2, 0, 0);
    }

    @Test
    public void testTimeoutBeforeFinish() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(rootTracer);
        asyncService.registerAsyncActivity(new HashMap<String, String>());

        assertNull(data);
        assertNull(stats);

        Thread.sleep(1000);

        // forces timeout to occur if time has passed
        asyncService.beforeHarvest("test", null);

        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        assertNull(data);
        assertNull(stats);

        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        Thread.sleep(500);

        assertNotNull(data);
        assertNotNull(stats);

        assertEquals(1, tx.getFinishedChildren().size());
        assertTokenMetricCounts(1, 0, 1, 0, 0);
    }

    @Test
    public void testTimeoutAllStrings() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(rootTracer);
        asyncService.registerAsyncActivity("123");
        asyncService.registerAsyncActivity("456");
        asyncService.registerAsyncActivity("789");
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        assertNull(data);
        assertNull(stats);

        Thread.sleep(1000);
        ServiceFactory.getTransactionService().processQueue();

        // forces timeout to occur if time has passed
        asyncService.beforeHarvest("test", null);
        // wait for async token timeouts to complete
        Thread.sleep(500);

        assertNotNull(data);
        assertNotNull(stats);

        assertEquals(1, tx.getFinishedChildren().size());
        assertTokenMetricCounts(3, 0, 3, 0, 0);
    }

    @Test
    public void testTimeoutNumbers() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(rootTracer);
        asyncService.registerAsyncActivity(1234L);
        asyncService.registerAsyncActivity(1234);
        asyncService.registerAsyncActivity(1234d);
        asyncService.registerAsyncActivity(1234f);
        asyncService.registerAsyncActivity(4L);
        asyncService.registerAsyncActivity(4f);
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        assertNull(data);
        assertNull(stats);

        Thread.sleep(1000);
        ServiceFactory.getTransactionService().processQueue();

        // forces timeout to occur if time has passed
        asyncService.beforeHarvest("test", null);
        // wait for async token timeouts to complete
        Thread.sleep(1000);

        assertNotNull(data);
        assertNotNull(stats);

        assertEquals(1, tx.getFinishedChildren().size());
        assertTokenMetricCounts(6, 0, 6, 0, 0);
    }

    /**
     * This test will not pass if the token time out is set to 0 (which effectively becomes 250ms) in createConfigMap
     * since the token will expire before all the setup work is done.
     */
    @Test
    public void testStartOneNormalAndOneNotStarted() throws Exception {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(rootTracer);

        String context1 = "123";
        asyncService.registerAsyncActivity(context1);
        Long context2 = 1234567L;
        asyncService.registerAsyncActivity(context2);
        tx.getTransactionActivity().tracerFinished(rootTracer, 0);

        assertNull(data);
        assertNull(stats);

        // Activity#run calls startAsyncActivity which does a link and expire, shown in the counts below
        Activity a1 = new Activity(context1);
        a1.start();
        a1.join();

        Thread.sleep(1000);
        ServiceFactory.getTransactionService().processQueue();
        // wait for async token timeouts to complete
        Thread.sleep(500);

        // forces timeout to occur if time has passed
        asyncService.beforeHarvest("test", null);

        assertNotNull(data);
        assertNotNull(stats);

        assertEquals(2, tx.getFinishedChildren().size());
        assertTokenMetricCounts(2, 1, 1, 0, 1);
    }

    class Activity extends Thread {
        Object context;

        public Activity(Object context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                Transaction tx = Transaction.getTransaction();
                Tracer rootTracer = createDispatcherTracer();
                tx.getTransactionActivity().tracerStarted(rootTracer);
                asyncService.startAsyncActivity(context);
                tx.getTransactionActivity().tracerFinished(rootTracer, 0);
            } catch (Exception e) {
                fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    // Create a Tracer for tests that require one.
    private BasicRequestRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
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

        runningTransactions.remove(transactionData.getTransaction());
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
    }

}
