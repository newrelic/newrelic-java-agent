/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({ Java16IncompatibleTest.class, Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class VertxFuture {

    @Test
    public void testCompositeFuture() throws InterruptedException {
        compositeFutures();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxFuture/compositeFutures";
        TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
        Map<String, Object> attributes = txnEvent.getAttributes();
        assertTrue(attributes.containsKey("success"));
    }

    @Trace(dispatcher = true)
    private void compositeFutures() throws InterruptedException {
        final Future<String> abc = Future.future();
        final Future<String> def = Future.future();

        final ExecutorService service = Executors.newSingleThreadExecutor();

        CountDownLatch latch = new CountDownLatch(1);
        CompositeFuture.all(abc, def).setHandler(result -> {
            if (result.succeeded()) {
                NewRelic.addCustomParameter("success", "yes");
            }
            latch.countDown();
        });

        service.submit(() -> {
            abc.complete("abc");
        });

        service.submit(() -> {
            def.complete("def");
        });

        latch.await();
    }

    @Test
    public void testFutureFail() throws InterruptedException {
        failFuture();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxFuture/failFuture";
        TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
        Map<String, Object> attributes = txnEvent.getAttributes();
        assertTrue(attributes.containsKey("future"));
    }

    @Trace(dispatcher = true)
    private void failFuture() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Future<String> future = Future.future();
        future.setHandler(ar -> {
            if (ar.failed()) {
                NewRelic.addCustomParameter("future", "failed");
                countDownLatch.countDown();
            }
        });

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            future.fail(new RuntimeException());
        });

        countDownLatch.await();
    }

}
