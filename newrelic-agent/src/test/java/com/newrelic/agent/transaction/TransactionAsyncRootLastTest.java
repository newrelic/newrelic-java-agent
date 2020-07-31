/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.TokenImpl;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionAsyncUtility;
import com.newrelic.agent.TransactionAsyncUtility.StartAndThenLink;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

public class TransactionAsyncRootLastTest implements TransactionListener {

    private TransactionData data;
    private TransactionStats stats;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        TransactionAsyncUtility.createServiceManager(createConfigMap());
    }

    @Before
    public void before() throws Exception {
        data = null;
        stats = null;
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    @After
    public void after() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    private static Map<String, Object> createConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(AgentConfigImpl.APP_NAME, "Unit Test");
        map.put(AgentConfigImpl.THREAD_CPU_TIME_ENABLED, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.GC_TIME_ENABLED, Boolean.TRUE);
        ttMap.put(TransactionTracerConfigImpl.TRANSACTION_THRESHOLD, 0.0f);
        map.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        return map;
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;
    }

    @Test
    public void testStartAndThenLinkExpireBeforeEnd() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        TransactionAsyncUtility.StartAndThenLink activity1 = new TransactionAsyncUtility.StartAndThenLink(token, true,
                false);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);

        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkExpireAfter() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        StartAndThenLink activity1 = new StartAndThenLink(token, false, true);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);

        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkExpireBoth() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        // second expire should do nothing
        StartAndThenLink activity1 = new StartAndThenLink(token, true, true);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);

        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkExpireInMain() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        // second expire should do nothing
        StartAndThenLink activity1 = new StartAndThenLink(token, false, false);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);
        token.expire();
        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkExpireInMainBefore() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        // second expire should do nothing
        StartAndThenLink activity1 = new StartAndThenLink(token, false, false);
        activity1.start();
        activity1.join();
        token.expire();
        rootTracer.finish(Opcodes.RETURN, 0);

        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkExpireAllBeforeFinish() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        // second expire should do nothing
        StartAndThenLink activity1 = new StartAndThenLink(token, false, false);
        activity1.start();
        activity1.join();
        tx.expireAllTokensForCurrentTransaction();
        rootTracer.finish(Opcodes.RETURN, 0);

        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkExpireAllFromToken() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        // second expire should do nothing
        StartAndThenLink activity1 = new StartAndThenLink(token, false, false);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);
        token.getTransaction().expireAllTokens();
        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkExpireAll() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token = (TokenImpl) tx.getToken();

        // second expire should do nothing
        StartAndThenLink activity1 = new StartAndThenLink(token, false, false);
        activity1.start();
        activity1.join();
        rootTracer.finish(Opcodes.RETURN, 0);
        // tx no longer on thread and so this will not work
        Transaction.getTransaction().expireAllTokensForCurrentTransaction();
        Assert.assertNull(data);
        Assert.assertNull(stats);
        // need to reference the tx directly
        tx.expireAllTokensForCurrentTransaction();

        verifyDataTwo(activity1, tx, token);
    }

    @Test
    public void testStartAndThenLinkMultipleDiffTokens() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "hi");
        tx.getTransactionActivity().tracerStarted(rootTracer);
        TokenImpl token1 = (TokenImpl) tx.getToken();
        TokenImpl token2 = (TokenImpl) tx.getToken();

        StartAndThenLink activity1 = new StartAndThenLink(token1, true, false);
        activity1.start();
        activity1.join();
        StartAndThenLink activity2 = new StartAndThenLink(token2, false, true);
        activity2.start();
        activity2.join();
        rootTracer.finish(Opcodes.RETURN, 0);

        waitForTransaction();
        TransactionAsyncUtility.basicDataVerify(data, stats, activity1, 3);

        Map<String, StatsBase> scoped = stats.getScopedStats().getStatsMap();
        ResponseTimeStats data1 = (ResponseTimeStats) scoped.get("RequestDispatcher");
        ResponseTimeStats data2 = (ResponseTimeStats) scoped.get("Java/java.lang.Object/root" + token1.toString());
        ResponseTimeStats data3 = (ResponseTimeStats) scoped.get("Java/java.lang.Object/root" + token2.toString());
        Assert.assertNotNull(data1);
        Assert.assertNotNull(data2);
        Assert.assertNotNull(data3);
        Assert.assertEquals(1, data1.getCallCount());
        Assert.assertEquals(1, data2.getCallCount());
        Assert.assertEquals(1, data3.getCallCount());

        Map<String, StatsBase> unscoped = stats.getUnscopedStats().getStatsMap();
        Assert.assertEquals(((ResponseTimeStats) unscoped.get("WebTransactionTotalTime")).getTotal(), data1.getTotal()
                + data2.getTotal() + data3.getTotal(), .001);
    }

    private void verifyDataTwo(StartAndThenLink activity, Transaction tx, TokenImpl token) {
        waitForTransaction();
        TransactionAsyncUtility.basicDataVerify(data, stats, activity, 2);

        Map<String, StatsBase> scoped = stats.getScopedStats().getStatsMap();
        ResponseTimeStats data1 = (ResponseTimeStats) scoped.get("RequestDispatcher");
        ResponseTimeStats data2 = (ResponseTimeStats) scoped.get("Java/java.lang.Object/root" + token.toString());
        Assert.assertNotNull(data1);
        Assert.assertNotNull(data2);
        Assert.assertEquals(1, data1.getCallCount());
        Assert.assertEquals(1, data2.getCallCount());

        Map<String, StatsBase> unscoped = stats.getUnscopedStats().getStatsMap();
        Assert.assertEquals(((ResponseTimeStats) unscoped.get("WebTransactionTotalTime")).getTotal(), data1.getTotal()
                + data2.getTotal(), .001);

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
