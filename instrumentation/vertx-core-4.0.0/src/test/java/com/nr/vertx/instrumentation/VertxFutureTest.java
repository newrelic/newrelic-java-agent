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
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class VertxFutureTest {

    @Test
    public void testCompositeFuture() throws InterruptedException {
        compositeFuturesAllFuturesSucceed();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxFutureTest/compositeFuturesAllFuturesSucceed";
        TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
        Map<String, Object> attributes = txnEvent.getAttributes();
        assertTrue(attributes.containsKey("compositeFuture"));
    }

    @Trace(dispatcher = true)
    private void compositeFuturesAllFuturesSucceed() throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch latch = new CountDownLatch(1);
        Promise<Object> promise1 = Promise.promise();
        Promise<Object> promise2 = Promise.promise();

        CompositeFuture.all(promise1.future(), promise2.future()).onComplete((ar -> {
            if (ar.succeeded()) {
                NewRelic.addCustomParameter("compositeFuture", "yes");
            }
            latch.countDown();
        }));

        vertx.setTimer(1, handler -> {
            promise1.complete("promise1");
            promise2.complete("promise2");
        });

        latch.await();
    }

    @Test
    public void whenFutureFails_withThrowable_txnStillCompletes() throws InterruptedException {
        failFutureWithThrowable();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxFutureTest/failFutureWithThrowable";
        TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
        Map<String, Object> attributes = txnEvent.getAttributes();
        assertTrue(attributes.containsKey("future"));
    }

    @Test
    public void whenFutureFails_withString_txnStillCompletes() throws InterruptedException {
        failFutureWithString();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxFutureTest/failFutureWithString";
        TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
        Map<String, Object> attributes = txnEvent.getAttributes();
        assertTrue(attributes.containsKey("future"));
    }

    @Test
    public void whenFutureCompletes_txnStillCompletes() throws InterruptedException {
        completeFutureSuccessfully();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxFutureTest/completeFutureSuccessfully";
        TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
        Map<String, Object> attributes = txnEvent.getAttributes();
        assertTrue(attributes.containsKey("future"));
    }

    @Test
    public void whenFutureCompletes_withOnCompleteRegistered_txnStillCompletes() throws InterruptedException {
        completeFutureSuccessfullyOnlyRegisteringOnCompleteCallback();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String expectedTxnName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxFutureTest/completeFutureSuccessfullyOnlyRegisteringOnCompleteCallback";
        TransactionEvent txnEvent = introspector.getTransactionEvents(expectedTxnName).iterator().next();
        Map<String, Object> attributes = txnEvent.getAttributes();
        assertTrue(attributes.containsKey("future"));
    }

    @Trace(dispatcher = true)
    private void failFutureWithString() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Vertx vertx = Vertx.vertx();
        Promise<Object> promise = Promise.promise();

        Future<Object> future = promise.future();
        future.onFailure(ar -> {
            NewRelic.addCustomParameter("future", "failed");
            countDownLatch.countDown();
        });

        vertx.setTimer(1, handler -> {
            promise.fail("oops");
        });

        countDownLatch.await();
    }

    @Trace(dispatcher = true)
    private void failFutureWithThrowable() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Vertx vertx = Vertx.vertx();
        Promise<Object> promise = Promise.promise();

        Future<Object> future = promise.future();
        future.onFailure(ar -> {
            NewRelic.addCustomParameter("future", "failed");
            countDownLatch.countDown();
        });

        vertx.setTimer(1, handler -> {
            promise.fail(new IllegalArgumentException("foo"));
        });

        countDownLatch.await();
    }

    @Trace(dispatcher = true)
    private void completeFutureSuccessfully() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Vertx vertx = Vertx.vertx();
        Promise<Object> promise = Promise.promise();

        Future<Object> future = promise.future();
        future.onSuccess(ar -> {
            NewRelic.addCustomParameter("future", "success");
            countDownLatch.countDown();
        });

        vertx.setTimer(1, handler -> {
            promise.complete("hooray");
        });


        countDownLatch.await();
    }

    @Trace(dispatcher = true)
    private void completeFutureSuccessfullyOnlyRegisteringOnCompleteCallback() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Vertx vertx = Vertx.vertx();
        Promise<Object> promise = Promise.promise();

        Future<Object> future = promise.future();
        future.onComplete(ar -> {
            NewRelic.addCustomParameter("future", "onComplete");
            countDownLatch.countDown();
        });

        vertx.setTimer(1, handler -> {
            promise.complete("hooray");
        });

        countDownLatch.await();
    }
}
