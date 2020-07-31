/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.TransactionAsyncUtility.Activity;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStatsImpl;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Async timeout is given in seconds. Sleep values need to be at least as long as the value passed into createConfigMap,
 * where 0s defaults to 250ms and 1s is 1000ms.
 */
public class TransactionAsyncEdgeCaseTest implements TransactionStatsListener {
    private TransactionData data;
    private TransactionStats stats;
    private List<TransactionStats> allStats = new CopyOnWriteArrayList<>();
    private int count = 0;

    @BeforeClass
    public static void beforeClass() throws Exception {
        TransactionAsyncUtility.createServiceManager(createConfigMap(0));
    }

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        count = 0;
        allStats.clear();
        ServiceFactory.getTransactionService().addTransactionStatsListener(this);
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionStatsListener(this);
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

    @Override
    public void dispatcherTransactionStatsFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;
        count++;
        allStats.add(transactionStats);
    }

    @Test
    public void testLinkSameThreadOneTracer() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Transaction.linkTxOnThread(token);
        token.expire();
        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(1, data.getTransactionActivities().size());
        Collection<Tracer> tracers = data.getTracers();
        Assert.assertEquals(1, tracers.size() + 1);
        Assert.assertEquals("RequestDispatcher", data.getRootTracer().getMetricName());
    }

    @Test
    public void testLinkAndExpireSameThreadOneTracer() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        token.linkAndExpire();
        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(1, data.getTransactionActivities().size());
        Collection<Tracer> tracers = data.getTracers();
        Assert.assertEquals(1, tracers.size() + 1);
        Assert.assertEquals("RequestDispatcher", data.getRootTracer().getMetricName());
    }

    @Test
    public void testLinkSameThreadTwoTracers() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Transaction.linkTxOnThread(token);

        Tracer defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        token.expire();
        defaultTracer.finish(Opcodes.RETURN, 0);
        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(1, data.getTransactionActivities().size());
        Collection<Tracer> tracers = data.getTracers();
        Assert.assertEquals(2, tracers.size() + 1);
        Iterator<Tracer> it = tracers.iterator();
        while (it.hasNext()) {
            Tracer t = it.next();
            Assert.assertEquals("Custom/mymethod", t.getMetricName());
        }
        Assert.assertEquals("RequestDispatcher", data.getRootTracer().getMetricName());

        Map<String, StatsBase> metrics = stats.getScopedStats().getStatsMap();
        ResponseTimeStatsImpl sb = (ResponseTimeStatsImpl) metrics.get("Custom/mymethod");
        Assert.assertEquals(1, sb.getCallCount());
        sb = (ResponseTimeStatsImpl) metrics.get("RequestDispatcher");
        Assert.assertEquals(1, sb.getCallCount());
    }

    @Test
    public void testLinkAndExpireSameThreadTwoTracers() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        token.linkAndExpire();

        Tracer defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        defaultTracer.finish(Opcodes.RETURN, 0);
        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(1, data.getTransactionActivities().size());
        Collection<Tracer> tracers = data.getTracers();
        Assert.assertEquals(2, tracers.size() + 1);
        Iterator<Tracer> it = tracers.iterator();
        while (it.hasNext()) {
            Tracer t = it.next();
            Assert.assertEquals("Custom/mymethod", t.getMetricName());
        }
        Assert.assertEquals("RequestDispatcher", data.getRootTracer().getMetricName());

        Map<String, StatsBase> metrics = stats.getScopedStats().getStatsMap();
        ResponseTimeStatsImpl sb = (ResponseTimeStatsImpl) metrics.get("Custom/mymethod");
        Assert.assertEquals(1, sb.getCallCount());
        sb = (ResponseTimeStatsImpl) metrics.get("RequestDispatcher");
        Assert.assertEquals(1, sb.getCallCount());
    }

    @Test
    public void testLinkSameThreadAfterWorkFinishes() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "outer-hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Tracer defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod1");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        defaultTracer.finish(Opcodes.RETURN, 0);
        rootTracer.finish(Opcodes.RETURN, 0);

        Transaction.clearTransaction();
        TransactionActivity.clear();

        TransactionActivity.create(null, 0);
        rootTracer = TransactionAsyncUtility.createOtherTracer("inner-hi");
        TransactionActivity.get().tracerStarted(rootTracer);

        Assert.assertTrue(Transaction.linkTxOnThread(token));
        defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod2");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        token.expire();
        defaultTracer.finish(Opcodes.RETURN, 0);
        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertEquals(2, data.getTransactionActivities().size());
        Collection<Tracer> tracers = data.getTracers();
        Assert.assertEquals(4, tracers.size() + 1);
        Assert.assertEquals("RequestDispatcher", data.getRootTracer().getMetricName());

        Map<String, StatsBase> metrics = stats.getScopedStats().getStatsMap();
        ResponseTimeStatsImpl sb = (ResponseTimeStatsImpl) metrics.get("Custom/mymethod1");
        Assert.assertEquals(1, sb.getCallCount());
        sb = (ResponseTimeStatsImpl) metrics.get("Custom/mymethod2");
        Assert.assertEquals(1, sb.getCallCount());
        sb = (ResponseTimeStatsImpl) metrics.get("Java/java.lang.Object/inner-hi");
        Assert.assertEquals(1, sb.getCallCount());
        sb = (ResponseTimeStatsImpl) metrics.get("RequestDispatcher");
        Assert.assertEquals(1, sb.getCallCount());
    }

    @Test
    public void testLinkAfterExpireTxNotInProgress() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "outer-hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Tracer defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod1");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        defaultTracer.finish(Opcodes.RETURN, 0);

        Thread.sleep(250);
        ServiceFactory.getTransactionService().processQueue();
        Thread.sleep(500);

        Assert.assertFalse(token.expire());
        rootTracer.finish(Opcodes.RETURN, 0);

        Assert.assertFalse(Transaction.linkTxOnThread(token));

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertFalse(Transaction.linkTxOnThread(token));
        Assert.assertEquals(1, data.getTransactionActivities().size());
    }

    @Test
    public void testLinkAfterExpireTxStillInProgress1() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "outer-hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Tracer defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod1");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        defaultTracer.finish(Opcodes.RETURN, 0);

        Thread.sleep(250);
        ServiceFactory.getTransactionService().processQueue();
        Thread.sleep(500);

        Assert.assertFalse(token.expire());
        TokenImpl token2 = (TokenImpl) tx.getToken();
        rootTracer.finish(Opcodes.RETURN, 0);

        TransactionActivity.clear();
        Transaction.clearTransaction();

        Assert.assertFalse(Transaction.linkTxOnThread(token));
        Assert.assertNull(data);
        Assert.assertNull(stats);

        TransactionActivity.create(null, 0);
        rootTracer = TransactionAsyncUtility.createOtherTracer("inner-hi");
        TransactionActivity.get().tracerStarted(rootTracer);
        Assert.assertTrue(Transaction.linkTxOnThread(token2));
        defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod2");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        token2.expire();
        defaultTracer.finish(Opcodes.RETURN, 0);
        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertFalse(Transaction.linkTxOnThread(token));
        Assert.assertEquals(2, data.getTransactionActivities().size());
    }

    @Test
    public void testLinkAfterExpireTxStillInProgress2() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "outer-hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Tracer defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod1");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        defaultTracer.finish(Opcodes.RETURN, 0);
        token.expire();

        Assert.assertNull(data);
        Assert.assertNull(stats);

        LinkFail activity1 = new LinkFail(token);
        activity1.start();
        activity1.join();

        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertFalse(activity1.linkResult);
        Assert.assertFalse(Transaction.linkTxOnThread(token));
        Assert.assertEquals(2, count);
    }

    public static class LinkFail extends Thread {
        private TokenImpl token;
        private boolean linkResult;

        public LinkFail(TokenImpl context) {
            this.token = context;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                TransactionActivity.clear();

                linkResult = token.link();

                Tracer rootTracer = TransactionAsyncUtility.createOtherTracer("root" + token.toString());
                Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);

                rootTracer.finish(Opcodes.RETURN, 0);

            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    public static class LinkWithWork extends Activity {
        private TokenImpl token;
        private boolean linkResult;

        public LinkWithWork(TokenImpl context) {
            this.token = context;
        }

        @Override
        public void run() {
            try {
                Transaction.clearTransaction();
                TransactionActivity.clear();

                // this should be a separate transaction
                Tracer rootTracer = TransactionAsyncUtility.createOtherTracer("root" + token.toString());
                Transaction.getTransaction().getTransactionActivity().tracerStarted(rootTracer);

                linkResult = token.link();
                // these are essentially asserts
                wasTxEqual = (token.getTransaction().getTransactionIfExists() == Transaction.getTransaction(false));
                isTxaNotNull = TransactionActivity.get() != null;

                token.expire();

                rootTracer.finish(Opcodes.RETURN, 0);

            } catch (Exception e) {
                Assert.fail("An exception should not have been thrown: " + e.getMessage());
            }
        }
    }

    @Test
    public void linkAndWithWorkCalled() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "outer-hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();
        Tracer defaultTracer = TransactionAsyncUtility.createDefaultTracer("mymethod1");
        tx.getTransactionActivity().tracerStarted(defaultTracer);
        defaultTracer.finish(Opcodes.RETURN, 0);
        // finish this txa
        rootTracer.finish(Opcodes.RETURN, 0);

        Assert.assertNull(data);
        Assert.assertNull(stats);

        LinkWithWork activity1 = new LinkWithWork(token);
        activity1.start();
        activity1.join();

        waitForTransaction();
        Assert.assertNotNull(data);
        Assert.assertNotNull(stats);

        Assert.assertTrue(activity1.linkResult);
        Assert.assertTrue(activity1.wasTxEqual);
        Assert.assertTrue(activity1.isTxaNotNull);
        Assert.assertEquals(1, count);
    }

    private void waitForTransaction() {
        long start = System.currentTimeMillis();
        // Wait for data to be available
        while ((System.currentTimeMillis() - start) < 5000 && (data == null || stats == null)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }


}
