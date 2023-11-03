/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import io.vertx.core.Vertx;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class VertxBlockingTest {

    @Test
    public void executeBlocking_withPromiseAndResult() throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        try {
            executeBlocking(vertx);
            String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxBlockingTest/executeBlocking";
            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            assertEquals(1, introspector.getFinishedTransactionCount(500));
            TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
            Map<String, Object> attributes = txnEvent.getAttributes();
            assertTrue(attributes.containsKey("InFuture"));
            assertTrue(attributes.containsKey("InResponseHandler"));
        } finally {
            vertx.close();
        }
    }

    @Trace(dispatcher = true)
    void executeBlocking(Vertx vertx) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        vertx.executeBlocking(future -> {
            NewRelic.addCustomParameter("InFuture", "yes");
            future.complete();
        }, res -> {
            NewRelic.addCustomParameter("InResponseHandler", "yes");
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }
}
