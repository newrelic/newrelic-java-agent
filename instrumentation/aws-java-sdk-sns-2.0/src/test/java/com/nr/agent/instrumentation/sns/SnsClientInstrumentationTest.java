/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sns;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon" })
public class SnsClientInstrumentationTest {
    private static final SnsClientMock mockSns = new SnsClientMock();
    private static final SnsAsyncClientMock asyncSnsMock = new SnsAsyncClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Test
    public void testPublishSync() throws Exception {
        PublishRequest request = PublishRequest.builder()
                .targetArn("bilbo").message("my message").subject("FREE REAL ESTATE NOW!").build();

        assertNotNull(doInTransaction(request));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/bilbo");
    }

    @Test
    public void testPublishAsync() throws Exception {
        PublishRequest request = PublishRequest.builder()
                .targetArn("bilbo").message("my message").subject("FREE REAL ESTATE NOW!").build();

        assertNotNull(doInAsyncTransaction(request));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/bilbo");
    }

    @Test
    public void testPublishASync_withHandler() throws Exception {
        PublishRequest request = PublishRequest.builder()
                .targetArn("bilbo").message("my message").subject("FREE REAL ESTATE NOW!").build();
        assertNotNull(doInAsyncTransaction(request));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/bilbo");
    }

    @Test
    public void testPublishASync_errorCase() throws Exception {
        PublishRequest request = PublishRequest.builder()
                .targetArn("frodo").message("fail me please").subject("FREE REAL ESTATE NOW!").build();

        assertNull(doInAsyncTransaction(request));

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/frodo");
    }

    @Test
    public void testPublishASync_errorCase_withCompleter() throws Exception {
        PublishRequest request = PublishRequest.builder()
                .targetArn("frodo").message("fail me please").subject("FREE REAL ESTATE NOW!").build();

        CompletableFuture<PublishResponse> result = publishAsync(request);
        AtomicBoolean properlyFailed = new AtomicBoolean();
        result.whenComplete((publishResponse, throwable) -> {
            if (throwable != null) {
                properlyFailed.set(true);
            }
        });

        verifyResults("MessageBroker/SNS/Topic/Produce/Named/frodo");
        assertTrue(properlyFailed.get());
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
    private PublishResponse doInTransaction(PublishRequest request) {
        return mockSns.publish(request);
    }

    @Trace(dispatcher = true)
    private PublishResponse doInAsyncTransaction(PublishRequest request) {
        CompletableFuture<PublishResponse> publishResultFuture = publishAsync(request);

        try {
            return publishResultFuture.get();
        } catch (Exception e) {
            return null;
        }
    }

    @Trace(dispatcher = true)
    private CompletableFuture<PublishResponse> publishAsync(PublishRequest request) {
        return asyncSnsMock.publish(request);
    }
}
