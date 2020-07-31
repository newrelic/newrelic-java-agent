/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.async;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @Trace( async = true) edge case test.
 * 
 *         1. @Trace(dispatcher = true).
 * 
 *         2. registerActivity.
 * 
 *         on a different thread, perform these two:
 * 
 *         3. @Trace( async = true) + startAsyncActivity with invalid context.
 * 
 *         4. @Trace( async = true) + startAsyncActivity with valid context.
 */
public class AsyncTracerTestEdgeCaseThree extends AsyncTest {

    @Test
    public void testAsyncEqualsTrue() throws InterruptedException {

        Thread secondThread = Txn();
        System.out.println("Awaiting termination");
        secondThread.join();

        // Wait for Transaction to finish (due to Token expiration on separate thread)
        Thread.sleep(5000);

        verifyScopedMetricsPresent("OtherTransaction/Custom/com.newrelic.agent.async.AsyncTracerTestEdgeCaseThree/Txn",
                "Java/com.newrelic.agent.async.AsyncTracerTestEdgeCaseThree/Txn",
                "Java/com.newrelic.agent.async.AsyncTracerTestEdgeCaseThree$1/tracerOne");

        ResponseTimeStats tracerOneStats = getStats().getScopedStats().getOrCreateResponseTimeStats(
                "Java/com.newrelic.agent.async.AsyncTracerTestEdgeCaseThree$1/tracerOne");
        Assert.assertNotNull("No stats for tracerOne", tracerOneStats);
        // first invocation of tracerOne (3) is outside tracked transaction.
        Assert.assertEquals("tracerOne was called a different number of times", 1, tracerOneStats.getCallCount());
    }

    // 1.
    @Trace(dispatcher = true)
    public static Thread Txn() {

        Runnable runnable = new Runnable() {
            @Trace(async = true)
            public void tracerOne(Object context) {
                System.out.println("Executing tracer one");
                AgentBridge.getAgent().startAsyncActivity(context);
            }

            @Override
            public void run() {
                // 3. invalid context on purpose. Not part of transaction created in 1.
                tracerOne(new Object());
                // 4.
                tracerOne(this);
            }
        };

        // 2.
        AgentBridge.getAgent().getTransaction().registerAsyncActivity(runnable);
        Thread secondThread = new Thread(runnable);
        secondThread.start();
        return secondThread;
    }
}
