/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;

public class AsyncTracerTest extends AsyncTest {

    @Test
    public void asyncInsideDispatcherTest() {
        asyncThenDispatcher();
        verifyScopedMetricsPresent("OtherTransaction/Custom/com.newrelic.agent.async.AsyncTracerTest/dispatcher");
    }

    @Trace(async = true)
    public static void asyncThenDispatcher() {
        dispatcher();
    }

    @Trace(dispatcher = true)
    public static void dispatcher() {
    }

    @Test
    public void startInSameThread() {
        final Object context = new Object();
        parent(context);
        child(context);
    }

    @Trace(dispatcher = true)
    public static void parent(Object context) {
        // System.out.println(TransactionActivity.get());
        AgentBridge.getAgent().getTransaction().registerAsyncActivity(context);
    }

    @Trace(async = true)
    public static void child(Object context) {
        // System.out.println(AgentBridge.getAgent().getTransaction(false));
        // System.out.println(TransactionActivity.get());
        AgentBridge.getAgent().startAsyncActivity(context);
        Assert.assertFalse(Transaction.getTransaction(false).isFinished());
    }
}
