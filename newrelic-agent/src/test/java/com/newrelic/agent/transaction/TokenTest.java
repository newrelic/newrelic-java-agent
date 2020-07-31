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
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.WeakRefTransaction;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.TransactionTracerConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TokenTest implements TransactionListener {

    private TransactionData data;
    private TransactionStats stats;

    @Before
    public void before() throws Exception {
        TransactionAsyncUtility.createServiceManager(createConfigMap(1));
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

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;
    }

    @Test
    public void testTracerNullAfterExpire() throws InterruptedException {
        Transaction.clearTransaction();
        Transaction tx = Transaction.getTransaction();
        Tracer rootTracer = TransactionAsyncUtility.createDispatcherTracer(this, "one");
        tx.getTransactionActivity().tracerStarted(rootTracer);

        TokenImpl token = (TokenImpl) tx.getToken();
        Assert.assertNotNull(token.getInitiatingTracer());
        Assert.assertNotNull(token.getTransaction());

        token.expire();
        waitForTransaction();
        Assert.assertNull(token.getInitiatingTracer());
        Assert.assertNotNull(token.getTransaction());
        Assert.assertTrue(token.getTransaction() instanceof WeakRefTransaction);
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
