/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.reactor.netty;

import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "reactor.netty", "reactor.core", "org.springframework" })
public class ReactorNettyTest {

    private static final int TIMEOUT = 3000;
    private static final String LIBRARY = "NettyReactor";

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @BeforeClass
    public static void before() {
        // This is here to prevent reactor.util.ConsoleLogger output from taking over your screen
        System.setProperty("reactor.logging.fallback", "JDK");
    }

    @Test
    public void testGetRequest() throws Exception {
        URI endpoint = server.getEndPoint();
        makeGetRequest(endpoint);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.reactor.netty.ReactorNettyTest/makeGetRequest";
        validateExternalCall(txnName, "GET", endpoint.getHost());
    }

    @Test
    public void testPostRequest() throws Exception {
        URI endpoint = server.getEndPoint();
        makePostRequest(endpoint);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.reactor.netty.ReactorNettyTest/makePostRequest";
        validateExternalCall(txnName, "POST", endpoint.getHost());
    }

    @Test
    public void testPutRequest() throws Exception {
        URI endpoint = server.getEndPoint();
        makePutRequest(endpoint);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.reactor.netty.ReactorNettyTest/makePutRequest";
        validateExternalCall(txnName, "PUT", endpoint.getHost());
    }

    @Test
    public void testDeleteRequest() throws Exception {
        URI endpoint = server.getEndPoint();
        makeDeleteRequest(endpoint);

        String txnName = "OtherTransaction/Custom/com.nr.instrumentation.reactor.netty.ReactorNettyTest/makeDeleteRequest";
        validateExternalCall(txnName, "DELETE", endpoint.getHost());
    }

    @Test
    public void testError() throws URISyntaxException {
        URI fakeUri = new URI("http://www.notarealhostbrosef.bro");
        try {
            makeGetRequest(fakeUri);
        } catch (Exception e) {
            // Expected exception
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        final String txnName = introspector.getTransactionNames().iterator().next();

        // Unknown hosts should create externals
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txnName);
        assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        assertEquals(LIBRARY, externalRequest.getLibrary());
    }

    @Test
    public void testNoTransaction() throws Exception {
        URI endpoint = server.getEndPoint();
        // Call without @Trace annotation - should not report externals
        HttpClient client = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));
        try {
            client.get()
                    .uri(endpoint.toString())
                    .response()
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            // Ignore - we're just testing that no externals are reported
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // Should have 1 transaction from the server only (no client transaction)
        assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));

        // Verify NO external calls were recorded (no client transaction to report into)
        String serverTxnName = introspector.getTransactionNames().iterator().next();
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(serverTxnName);
        assertEquals(0, externalRequests.size());
    }

    @Trace(dispatcher = true)
    public void makeGetRequest(URI uri) throws Exception {
        System.out.println("[TEST] Starting makeGetRequest");
        HttpClient client = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        System.out.println("[TEST] About to make HTTP call");
        client.get()
                .uri(uri.toString())
                .response()
                .block(Duration.ofSeconds(5));
        System.out.println("[TEST] HTTP call completed");
    }

    @Trace(dispatcher = true)
    public void makePostRequest(URI uri) throws Exception {
        HttpClient client = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        client.post()
                .uri(uri.toString())
                .response()
                .block(Duration.ofSeconds(5));
    }

    @Trace(dispatcher = true)
    public void makePutRequest(URI uri) throws Exception {
        HttpClient client = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        client.put()
                .uri(uri.toString())
                .response()
                .block(Duration.ofSeconds(5));
    }

    @Trace(dispatcher = true)
    public void makeDeleteRequest(URI uri) throws Exception {
        HttpClient client = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        client.delete()
                .uri(uri.toString())
                .response()
                .block(Duration.ofSeconds(5));
    }

    private void validateExternalCall(String txnName, String httpMethod, String host) {
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // Expect 2 transactions: 1 from client (our test), 1 from server (HttpServerRule)
        assertEquals(2, introspector.getFinishedTransactionCount(TIMEOUT));
        assertTrue(introspector.getTransactionNames().contains(txnName));

        // Verify external request details
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txnName);
        Assert.assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        Assert.assertEquals(LIBRARY, externalRequest.getLibrary());
        Assert.assertEquals(httpMethod, externalRequest.getOperation());
        Assert.assertEquals(host, externalRequest.getHostname());

        // Verify scoped metrics
        String metricName = "External/" + host + "/" + LIBRARY + "/" + httpMethod;
        Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(txnName, metricName));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount(metricName));

        // Verify external rollup metrics
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));

        // Verify transaction event attributes
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txnName);
        Assert.assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        Assert.assertEquals(1, transactionEvent.getExternalCallCount());
        Assert.assertTrue(transactionEvent.getExternalDurationInSec() > 0);
    }
}