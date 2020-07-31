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

public class AsyncTracerEdgeCaseTwo extends AsyncTest {

    @Test
    public void testTracedMethodFirst() throws InterruptedException {

        Thread secondThread = Txn();
        System.out.println("Awaiting termination");
        secondThread.join();

        verifyScopedMetricsPresent("OtherTransaction/MyTxn/namedByasyncTracerMethod",
                "Java/com.newrelic.agent.async.AsyncTracerEdgeCaseTwo/Txn",
                "Java/com.newrelic.agent.async.AsyncTracerEdgeCaseTwo$1/asyncTracerMethod");
    }

    @Trace(dispatcher = true)
    public static Thread Txn() {

        Runnable runnable = new Runnable() {

            @Trace
            public void tracedMethod() {
                System.out.println("Executing traced method.");
                NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true,
                        "Other", "thisShouldNotBeReported");
            }

            @Trace(async = true)
            public void asyncTracerMethod(Object context) {
                System.out.println("Executing async tracer method");
                AgentBridge.getAgent().startAsyncActivity(context);
                NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                        "MyTxn", "namedByasyncTracerMethod");
            }

            @Override
            public void run() {
                // tracedMethod puts a txn on this thread by calling getTransaction(), which shouldn't
                // affect next tracer (asyncTracerMethod).
                tracedMethod();
                asyncTracerMethod(this);
                tracedMethod();
            }
        };

        AgentBridge.getAgent().getTransaction().registerAsyncActivity(runnable);
        Thread secondThread = new Thread(runnable);
        secondThread.start();
        return secondThread;
    }
}
