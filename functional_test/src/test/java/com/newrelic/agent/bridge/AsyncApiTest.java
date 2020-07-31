/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import org.junit.Assert;
import org.junit.Test;

import com.newrelic.api.agent.Trace;

public class AsyncApiTest {

    @Test
    public void test() throws Exception {

        final Object asyncContext = new Object();

        final Transaction primaryTx = new Callable<Transaction>() {

            @Trace(dispatcher = true)
            @Override
            public Transaction call() {
                Transaction tx = AgentBridge.getAgent().getTransaction(); // TX-1

                AgentBridge.asyncApi.suspendAsync(asyncContext);

                return tx;
            }
        }.call();

        int primaryTxHashCode = primaryTx.hashCode(); // TX-1 hash

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Callable<Integer> work = new Callable<Integer>() {

                @Trace(dispatcher = true)
                @Override
                public Integer call() {
                    Transaction secondTx = AgentBridge.getAgent().getTransaction(); // TX-2

                    AgentBridge.asyncApi.resumeAsync(asyncContext); // TX-1

                    Transaction resumedTx = AgentBridge.getAgent().getTransaction();
                    Assert.assertEquals(primaryTx, resumedTx); // both must be TX-1

                    return secondTx.hashCode();
                }
            };

            int secondTxHashCode = executor.submit(work).get(30, TimeUnit.SECONDS);

            // Change for async overhaul. Before the async overhaul, the test checked that
            // the primary and secondary transactions were different, i.e. that TX-1 from
            // the first callable was not the same as TX-2 from the second callable. During
            // async overhaul, the implementation of AgentBridge.getAgent().getTransaction()
            // was altered to return the stateless wrapper (currently TransactionApiImpl,
            // although we may change the name of the implementation class). As a result
            // comparisons like this one, which is the old code from the test ...
            // Assert.assertNotEquals(primaryTx, secondTx);
            // ...no longer work as expected, because both wrappers dynamically grab the
            // transaction on the current thread at the time the assertion is executed.
            //
            // We can still make the same assertion, but we have to grab a representation
            // of the transaction while it's active on the current thread, and hence the
            // use of hash codes:
            Assert.assertNotEquals(primaryTxHashCode, secondTxHashCode);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSupportabilityMetrics() throws Exception {

        String suspend = "Supportability/API/LegacyAsync/Suspend";
        String resume = "Supportability/API/LegacyAsync/Resume";

        final Object asyncContext = new Object();

        final Transaction primaryTx = new Callable<Transaction>() {

            @Trace(dispatcher = true)
            @Override
            public Transaction call() {
                Transaction tx = AgentBridge.getAgent().getTransaction(); // TX-1

                AgentBridge.asyncApi.suspendAsync(asyncContext);

                return tx;
            }
        }.call();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Callable<Integer> work = new Callable<Integer>() {

                @Trace(dispatcher = true)
                @Override
                public Integer call() {
                    Transaction secondTx = AgentBridge.getAgent().getTransaction(); // TX-2

                    AgentBridge.asyncApi.resumeAsync(asyncContext); // TX-1

                    Transaction resumedTx = AgentBridge.getAgent().getTransaction();
                    Assert.assertEquals(primaryTx, resumedTx); // both must be TX-1

                    return secondTx.hashCode();
                }
            };

            Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
            Assert.assertNotNull("suspend", metricData.get(suspend));
        } finally {
            executor.shutdown();
        }
    }
}
