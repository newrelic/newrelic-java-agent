/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Trace;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * This class is the regression test for JAVA-2675, the root cause of Zendesk #201125.
 */
public class SequentialTracerTest implements TransactionListener {

    protected TransactionData data;
    protected List<TransactionData> dataList;
    protected TransactionStats stats;
    protected List<TransactionStats> statsList;
    private int timesSet;

    @Before
    public void setup() {
        ServiceFactory.getTransactionService().addTransactionListener(this);

        data = null;
        dataList = new ArrayList<>();
        stats = null;
        statsList = new ArrayList<>();
        timesSet = 0;
    }

    @After
    public void unregister() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    @Test
    public void test_D() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                methodWithDispatcherTracer();
            }
        };

        runAssert(t1, 1);
    }


    @Test
    public void test_F() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                AgentBridge.getAgent().getLogger().log(Level.INFO, "TEST THREAD: test_F() starting...");
                methodWithFlyweightTracer();
                AgentBridge.getAgent().getLogger().log(Level.INFO, "TEST THREAD: test_F() done.");
            }
        };

        runAssert(t1, 0);
    }

    @Test
    public void test_A() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                methodWithAsyncTrueTracer();
            }
        };

        runAssert(t1, 0);
    }

    @Test
    public void test_D_A() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                methodWithDispatcherTracer();
                methodWithAsyncTrueTracer();
            }
        };

        runAssert(t1, 1);
    }

    @Test
    public void test_D_F() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                methodWithDispatcherTracer();
                methodWithFlyweightTracer();
            }
        };

        runAssert(t1, 1);
    }

    @Test
    public void test_F_A() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                AgentBridge.getAgent().getLogger().log(Level.INFO, "TEST THREAD: test_F_A() starting...");
                methodWithFlyweightTracer();
                methodWithAsyncTrueTracer();
                AgentBridge.getAgent().getLogger().log(Level.INFO, "TEST THREAD: test_F_A() done.");
            }
        };

        runAssert(t1, 0);
    }

    @Test
    public void test_D_F_A() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                methodWithDispatcherTracer();
                methodWithFlyweightTracer();
                methodWithAsyncTrueTracer();
            }
        };

        runAssert(t1, 1);
    }

    @Test
    public void test_D_F_A_D() throws Exception {
        Thread t1 = new Thread() {
            @Override
            public void run() {
                methodWithDispatcherTracer();
                methodWithFlyweightTracer();
                methodWithAsyncTrueTracer();
                methodWithDispatcherTracer();
            }
        };

        runAssert(t1, 2);
    }

    @Trace(leaf = true, excludeFromTransactionTrace = true)
    void methodWithFlyweightTracer() {
    }

    @Trace(async = true)
    void methodWithAsyncTrueTracer() {
    }

    @Trace(dispatcher = true)
    void methodWithDispatcherTracer() {
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        data = transactionData;
        stats = transactionStats;
        timesSet++;
        dataList.add(transactionData);
        statsList.add(transactionStats);
    }

    // Run the thread, join it, and assert the number of transactions is performed.
    private void runAssert(Thread toRun, int nTransactions) throws InterruptedException {
        toRun.start();
        toRun.join();
        Assert.assertTrue(timesSet == nTransactions);
    }
}