/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sns;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.amazonaws" })
public class AmazonSNSInstrumentationTest {
    private static final AmazonSNSMock mockSns = new AmazonSNSMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Test
    public void testPublishSync() throws Exception {
        PublishRequest request = new PublishRequest("bilbo", "my message", "FREE REAL ESTATE NOW!");

        assertNotNull(doInTransaction(mockSns, request));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/bilbo");
    }

    @Test
    public void testPublishASync() throws Exception {
        PublishRequest request = new PublishRequest("bilbo", "my message", "FREE REAL ESTATE NOW!");

        assertNotNull(doInAsyncTransaction(mockSns, request));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/bilbo");
    }

    @Test
    public void testPublishASync_withHandler() throws Exception {
        PublishRequest request = new PublishRequest("bilbo", "my message", "FREE REAL ESTATE NOW!");
        VerifyingHandler verifyingHandler = new VerifyingHandler();
        assertNotNull(doInAsyncTransaction(mockSns, request, verifyingHandler));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/bilbo");
        assertTrue(verifyingHandler.onSuccessHit.get());
        assertFalse(verifyingHandler.onErrorHit.get());
    }

    @Test
    public void testPublishASync_errorCase() throws Exception {
        PublishRequest request = new PublishRequest("frodo", "fail me please", "FREE REAL ESTATE NOW!");

        assertNotNull(doInAsyncTransaction(mockSns, request));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/frodo");
    }

    @Test
    public void testPublishASync_errorCase_withHandler() throws Exception {
        PublishRequest request = new PublishRequest("frodo", "fail me please", "FREE REAL ESTATE NOW!");
        VerifyingHandler verifyingHandler = new VerifyingHandler();

        assertNotNull(doInAsyncTransaction(mockSns, request, verifyingHandler));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/frodo");
        assertFalse(verifyingHandler.onSuccessHit.get());
        assertTrue(verifyingHandler.onErrorHit.get());
    }

    private void verifyResults(String metricName) {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertTrue(metrics.containsKey(metricName));
        assertEquals(1, metrics.get(metricName).getCallCount());
    }

    @Trace(dispatcher = true)
    private PublishResult doInTransaction(AmazonSNSMock mockSns, PublishRequest request) {
        return mockSns.publish(request);
    }

    @Trace(dispatcher = true)
    private PublishResult doInAsyncTransaction(AmazonSNSMock mockSns, PublishRequest request) throws InterruptedException, ExecutionException {
        Future<PublishResult> publishResultFuture = mockSns.publishAsync(request);

        return publishResultFuture.get();
    }

    @Trace(dispatcher = true)
    private PublishResult doInAsyncTransaction(AmazonSNSMock mockSns, PublishRequest request,
            AsyncHandler<PublishRequest, PublishResult> asyncHandler) throws InterruptedException, ExecutionException {
        Future<PublishResult> publishResultFuture = mockSns.publishAsync(request, asyncHandler);

        return publishResultFuture.get();
    }

    private static class VerifyingHandler implements AsyncHandler<PublishRequest, PublishResult> {
        private final AtomicBoolean onErrorHit = new AtomicBoolean();
        private final AtomicBoolean onSuccessHit = new AtomicBoolean();

        @Override
        public void onError(Exception exception) {
            onErrorHit.set(true);
        }

        @Override
        public void onSuccess(PublishRequest request, PublishResult publishResult) {
            onSuccessHit.set(true);
        }

    }
}
