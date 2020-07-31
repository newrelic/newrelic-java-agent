/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import org.junit.Test;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;

public class AsyncTracerEdgeCase extends AsyncTest {

    @Test
    public void testTracedMethodFirst() throws InterruptedException {
        asyncTracerMethod();
        startTxn();
        verifyScopedMetricsPresent("OtherTransaction/Other/thisShouldBeReported",
                "Java/com.newrelic.agent.async.AsyncTracerEdgeCase/startTxn",
                "Custom/com.newrelic.agent.async.AsyncTracerEdgeCase/tracedMethod");
    }

    @Trace(async = true)
    public static void asyncTracerMethod() {
        AgentBridge.getAgent().startAsyncActivity(new Object());
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                "txnName", "thisShouldNotBeReported");
    }

    @Trace(dispatcher = true)
    public static void startTxn() {
        tracedMethod();
    }

    @Trace
    public static void tracedMethod() {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Other",
                "thisShouldBeReported");
    }
}
