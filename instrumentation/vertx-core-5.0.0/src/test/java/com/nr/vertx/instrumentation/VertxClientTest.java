/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.internal.HttpServerLocator;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class VertxClientTest {

    private static int port;
    private static Future<HttpServer> server;
    private static Vertx vertx;

    @BeforeClass
    public static void beforeClass() {
        port = getAvailablePort();
        vertx = Vertx.vertx();

        server = vertx.createHttpServer().requestHandler(request -> {
            final String statusCode = request.getHeader("statusCode");
            if (statusCode == null) {
                request.response().end("response");
            } else {
                if (request.absoluteURI().equals("/redirect")) {
                    request.headers().clear();
                    request.response().putHeader("Location", "http://localhost:" + port + "/other");
                }
                request.response().setStatusCode(Integer.parseInt(statusCode)).end("response");
            }
        }).listen(port);
    }

    @AfterClass
    public static void afterClass() {
        server.result().close();
        vertx.close();
    }

    @Test
    public void testGet_withFutures() throws InterruptedException {
        getCall_withFutures();
        // Wait for transaction to finish
        InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(1000);
        assertExternal(
                "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClientTest/getCall_withFutures",
                "localhost");
    }

    @Trace(dispatcher = true)
    public void getCall_withFutures() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();

        // In Vert.x 5.0.0, HttpClient.request() returns Future<HttpClientRequest> directly.
        httpClient
                .request(HttpMethod.GET, port, "localhost", "/")
                .onSuccess(
                        request -> {
                            request.send()
                                    .onSuccess(
                                            response -> {
                                                NewRelic.addCustomParameter("responseHandler", "true");
                                                latch.countDown();
                                            })
                                    .onFailure(err -> latch.countDown());
                        })
                .onFailure(err -> latch.countDown());
        latch.await();
    }

    @Test
    public void testPost() throws InterruptedException {
        postCall();
        // Wait for transaction to finish
        InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(5000);
        assertExternal(
                "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClientTest/postCall",
                "localhost");
    }

    @Trace(dispatcher = true)
    private void postCall() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();

        httpClient
                .request(HttpMethod.POST, port, "localhost", "/")
                .onSuccess(
                        request -> {
                            request.send()
                                    .onSuccess(
                                            response -> {
                                                NewRelic.addCustomParameter("responseHandler", "true");
                                                latch.countDown();
                                            })
                                    .onFailure(err -> latch.countDown());
                        })
                .onFailure(err -> latch.countDown());
        latch.await();
    }

    @Test
    public void testCat() throws Exception {
        try (HttpTestServer httpServer = HttpServerLocator.createAndStart()) {
            cat(httpServer);

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            String host = httpServer.getEndPoint().getHost();
            String txName =
                    "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClientTest/cat";
            assertEquals(2, introspector.getFinishedTransactionCount(250));
            Collection<String> names = introspector.getTransactionNames();
            assertEquals(2, names.size());
            assertTrue(names.contains(httpServer.getServerTransactionName()));
            assertTrue(names.contains(txName));

            // scoped metrics
            assertEquals(
                    1,
                    MetricsHelper.getScopedMetricCount(
                            txName,
                            "ExternalTransaction/"
                                    + host
                                    + "/"
                                    + httpServer.getCrossProcessId()
                                    + "/"
                                    + httpServer.getServerTransactionName()));
            assertEquals(
                    1,
                    MetricsHelper.getScopedMetricCount(
                            txName, "Java/com.nr.vertx.instrumentation.VertxClientTest/cat"));

            // unscoped metrics
            assertEquals(
                    1,
                    MetricsHelper.getUnscopedMetricCount(
                            "ExternalTransaction/"
                                    + host
                                    + "/"
                                    + httpServer.getCrossProcessId()
                                    + "/"
                                    + httpServer.getServerTransactionName()));
            assertEquals(
                    1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));

            // events
            Collection<TransactionEvent> transactionEvents =
                    introspector.getTransactionEvents(txName);
            assertEquals(1, transactionEvents.size());
            TransactionEvent transactionEvent = transactionEvents.iterator().next();
            assertEquals(1, transactionEvent.getExternalCallCount());
            assertTrue(transactionEvent.getExternalDurationInSec() > 0);

            CatHelper.verifyOneSuccessfulCat(introspector, txName);

            Collection<ExternalRequest> externalRequests =
                    introspector.getExternalRequests(txName);
            assertEquals(1, externalRequests.size());
            ExternalRequest externalRequest = externalRequests.iterator().next();
            assertEquals(1, externalRequest.getCount());
            assertEquals(host, externalRequest.getHostname());
        }
    }

    @Trace(dispatcher = true)
    public void cat(HttpTestServer httpServer) throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            HttpClient httpClient = vertx.createHttpClient();
            httpClient
                    .request(
                            HttpMethod.GET,
                            httpServer.getEndPoint().getPort(),
                            httpServer.getEndPoint().getHost(),
                            "/")
                    .onSuccess(
                            request -> {
                                request.putHeader(HttpTestServer.DO_CAT, "true");
                                request.send()
                                        .onSuccess(
                                                response -> {
                                                    NewRelic.addCustomParameter(
                                                            "responseHandler", "true");
                                                    latch.countDown();
                                                })
                                        .onFailure(err -> latch.countDown());
                            })
                    .onFailure(err -> latch.countDown());
            latch.await();
        } finally {
            httpServer.shutdown();
        }
    }

    @Test
    public void testUnknownHost_withReturnedFuture() throws Exception {
        unknownHost_withReturnedFuture();
        assertUnknownHostExternal();
    }

    @Trace(dispatcher = true)
    private void unknownHost_withReturnedFuture() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();

        Future<HttpClientRequest> r =
                httpClient.request(HttpMethod.GET, port, "notARealHostDuderina.com", "/");
        r.onFailure(
                err -> {
                    NewRelic.addCustomParameter("exceptionHandler", "true");
                    latch.countDown();
                });

        latch.await();
    }

    private void assertUnknownHostExternal() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(250));

        final String txn = introspector.getTransactionNames().iterator().next();
        assertNotNull("Transaction not found", txn);

        assertEquals(
                1,
                MetricsHelper.getScopedMetricCount(
                        txn, "External/UnknownHost/Vertx-Client/handleResponse"));
        assertEquals(
                1,
                MetricsHelper.getUnscopedMetricCount(
                        "External/UnknownHost/Vertx-Client/handleResponse"));

        // Unknown hosts generate no external rollups
        assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));

        final TransactionEvent event =
                introspector.getTransactionEvents(txn).iterator().next();
        assertTrue(event.getAttributes().containsKey("exceptionHandler"));
    }

    private void assertExternal(String transactionName, String host) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<ExternalRequest> externalRequests =
                introspector.getExternalRequests(transactionName);
        ExternalRequest request = externalRequests.iterator().next();
        assertEquals(host, request.getHostname());
        assertEquals("Vertx-Client", request.getLibrary());
        assertEquals("handleResponse", request.getOperation());
        Collection<TransactionEvent> events =
                introspector.getTransactionEvents(transactionName);
        TransactionEvent event = events.iterator().next();
        assertTrue(event.getAttributes().containsKey("responseHandler"));

        assertEquals(
                1,
                MetricsHelper.getScopedMetricCount(
                        transactionName,
                        "External/localhost/Vertx-Client/handleResponse"));
        assertEquals(
                1,
                MetricsHelper.getUnscopedMetricCount(
                        "External/localhost/Vertx-Client/handleResponse"));

        Collection<TransactionEvent> transactionEvents =
                introspector.getTransactionEvents(transactionName);
        assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        assertEquals(1, transactionEvent.getExternalCallCount());
        assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        // external rollups
        assertEquals(
                1, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    private static int getAvailablePort() {
        int port;

        try {
            ServerSocket socket = new ServerSocket(0);
            port = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to allocate ephemeral port");
        }
        return port;
    }
}
