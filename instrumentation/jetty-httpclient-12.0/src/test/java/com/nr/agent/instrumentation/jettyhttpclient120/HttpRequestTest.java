/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jettyhttpclient120;

import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.client.transport.HttpRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.eclipse.jetty.client.transport" })
public class HttpRequestTest {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @After
    public void reset() {
        InstrumentationTestRunner.getIntrospector().clear();
    }

    @Test
    public void testSend() throws Exception {
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();

        exerciseSend(endpoint.toString());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // Two transactions: the client transaction and the server transaction
        int txCount = introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10));
        Assert.assertEquals(2, txCount);

        String txName = null;
        for (String name : introspector.getTransactionNames()) {
            if (name.contains("HttpRequestTest")) {
                txName = name;
            }
        }
        Assert.assertNotNull("Client transaction not found", txName);

        // Scoped and unscoped External metrics
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "External/" + host + "/Jetty HttpClient/send"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount(
                "External/" + host + "/Jetty HttpClient/send"));

        // External rollup metrics
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));

        // Transaction event attributes
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
        Assert.assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        Assert.assertEquals(1, transactionEvent.getExternalCallCount());
        Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        // ExternalRequest attributes
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);
        Assert.assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        Assert.assertEquals(1, externalRequest.getCount());
        Assert.assertEquals(host, externalRequest.getHostname());
        Assert.assertEquals("Jetty HttpClient", externalRequest.getLibrary());
        Assert.assertEquals("send", externalRequest.getOperation());
    }

    @Test
    public void testSendUnknownHost() throws Exception {
        final String unknownHost = "www.notarealhostbrosef.bro";
        try {
            exerciseSend("http://" + unknownHost);
            Assert.fail("Host should not be reachable: " + unknownHost);
        } catch (Exception expected) {
            // expected — unreachable host
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10)));
        String txName = introspector.getTransactionNames().iterator().next();

        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "External/UnknownHost/Jetty HttpClient/send"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount(
                "External/UnknownHost/Jetty HttpClient/send"));

        // Unknown hosts should not generate rollup metrics
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Test
    public void testNotify() throws Exception {
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();

        exerciseNotify(endpoint.toString());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // Two transactions: the client transaction and the server transaction
        int txCount = introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10));
        Assert.assertEquals(2, txCount);

        String txName = null;
        for (String name : introspector.getTransactionNames()) {
            if (name.contains("HttpRequestTest")) {
                txName = name;
            }
        }
        Assert.assertNotNull("Client transaction not found", txName);

        // The send() instrumentation should still produce an External metric even
        // when exercised via the async listener path
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Trace(dispatcher = true)
    private void exerciseSend(String url) throws Exception {
        HttpClient client = new HttpClient();
        client.start();
        try {
            // synchronous send — internally calls send(Response.CompleteListener) under the hood
            ContentResponse response = client.GET(url);
            Assert.assertNotNull(response);
        } finally {
            client.stop();
        }
    }

    @Trace(dispatcher = true)
    private void exerciseNotify(String url) throws Exception {
        HttpClient client = new HttpClient();
        client.start();
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Response> responseRef = new AtomicReference<Response>();

            // send() with an explicit CompleteListener triggers the notify* path
            client.newRequest(URI.create(url))
                    .send(new Response.CompleteListener() {
                        public void onComplete(Result result) {
                            responseRef.set(result.getResponse());
                            latch.countDown();
                        }
                    });

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            Assert.assertTrue("Request did not complete within timeout", completed);
            Assert.assertNotNull(responseRef.get());
        } finally {
            client.stop();
        }
    }
}
