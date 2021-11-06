/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({ Java16IncompatibleTest.class, Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class VertxClient {

    private static int port;
    private static Vertx vertx;
    private static HttpServer server;

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
        server.close();
        vertx.close();
    }

    @Test
    public void testGet() throws InterruptedException {
        getCall();
        // Wait for transaction to finish
        InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(5000);
        assertExternal("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClient/getCall", "localhost");
    }

    @Trace(dispatcher = true)
    public void getCall() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();
        HttpClientRequest request = httpClient.options(port, "localhost", "/").handler(requestHandler(latch));
        request.end();
        latch.await();
    }

    @Test
    public void testGetNow() throws InterruptedException {
        getNowCall();
        // Wait for transaction to finish
        InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(5000);
        assertExternal("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClient/getNowCall", "localhost");
    }

    @Trace(dispatcher = true)
    private void getNowCall() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();
        httpClient.getNow(port, "localhost", "/", requestHandler(latch));
        latch.await();
    }

    @Test
    public void testPost() throws InterruptedException {
        postCall();
        // Wait for transaction to finish
        InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(5000);
        assertExternal("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClient/postCall", "localhost");
    }

    @Trace(dispatcher = true)
    private void postCall() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();
        httpClient.post(port, "localhost", "/", requestHandler(latch)).end();
        latch.await();
    }

    @Test
    public void testEndMethods() throws InterruptedException {
        endMethods();
        assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(1000));
        final ExternalRequest externalRequest = InstrumentationTestRunner.getIntrospector().getExternalRequests(
                "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClient/endMethods").iterator().next();
        assertEquals(4, externalRequest.getCount());
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(4, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Trace(dispatcher = true)
    public void endMethods() throws InterruptedException {
        HttpClient httpClient = vertx.createHttpClient();
        CountDownLatch latch = new CountDownLatch(4);

        Buffer bufferChunk = Buffer.buffer("buffer chunk!");
        String stringChunk = "string chunk!";
        String encoding = "UTF-8";

        // tests the various overloaded versions of the end method
        httpClient.request(HttpMethod.GET, port, "localhost", "/hi", requestHandler(latch)).end();
        httpClient.request(HttpMethod.GET, port, "localhost", "/hi", requestHandler(latch)).end(bufferChunk);
        httpClient.request(HttpMethod.GET, port, "localhost", "/hi", requestHandler(latch)).end(stringChunk, encoding);
        httpClient.request(HttpMethod.GET, port, "localhost", "/hi", requestHandler(latch)).end(stringChunk);
        latch.await();
    }

    @Test
    public void testMethods() throws InterruptedException {
        requestMethods();
        assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(1000));
        final ExternalRequest externalRequest = InstrumentationTestRunner.getIntrospector().getExternalRequests(
                "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClient/requestMethods").iterator().next();
        assertEquals(5, externalRequest.getCount());
        assertEquals(5, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
        assertEquals(5, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(5, MetricsHelper.getUnscopedMetricCount("External/all"));
    }

    @Trace(dispatcher = true)
    public void requestMethods() throws InterruptedException {
        HttpClient httpClient = vertx.createHttpClient();
        CountDownLatch latch = new CountDownLatch(5);
        httpClient.request(HttpMethod.GET, port, "localhost", "/hi", requestHandler(latch)).end();
        httpClient.request(HttpMethod.POST, port, "localhost", "/hi", requestHandler(latch)).end();
        httpClient.request(HttpMethod.PUT, port, "localhost", "/hi", requestHandler(latch)).end();
        httpClient.request(HttpMethod.HEAD, port, "localhost", "/hi", requestHandler(latch)).end();
        httpClient.request(HttpMethod.DELETE, port, "localhost", "/hi", requestHandler(latch)).end();
        latch.await();
    }

    @Test
    public void testRedirect() throws InterruptedException {
        redirect();
        // Wait for transaction to finish
        InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(5000);
        assertExternal("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClient/redirect", "localhost");
    }

    @Trace(dispatcher = true)
    public void redirect() throws InterruptedException {
        HttpClient httpClient = vertx.createHttpClient();
        CountDownLatch latch = new CountDownLatch(1);
        httpClient.get(port, "localhost", "/redirect")
                  .putHeader("statusCode", "301")
                  .handler(requestHandler(latch))
                  .end();
        latch.await();
    }


    @Test
    public void testCat() throws Exception {
        try (HttpTestServer httpServer = HttpServerLocator.createAndStart()) {
            cat(httpServer);

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            String host = httpServer.getEndPoint().getHost();
            String txName = "OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxClient/cat";
            assertEquals(2, introspector.getFinishedTransactionCount(250));
            Collection<String> names = introspector.getTransactionNames();
            assertEquals(2, names.size());
            assertTrue(names.contains(httpServer.getServerTransactionName()));
            assertTrue(names.contains(txName));

            // scoped metrics
            assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                    + httpServer.getCrossProcessId() + "/" + httpServer.getServerTransactionName()));
            assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                    "Java/com.nr.vertx.instrumentation.VertxClient/cat"));

            // unscoped metrics
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("ExternalTransaction/" + host + "/"
                    + httpServer.getCrossProcessId() + "/" + httpServer.getServerTransactionName()));
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));

            // events
            Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
            assertEquals(1, transactionEvents.size());
            TransactionEvent transactionEvent = transactionEvents.iterator().next();
            assertEquals(1, transactionEvent.getExternalCallCount());
            assertTrue(transactionEvent.getExternalDurationInSec() > 0);

            CatHelper.verifyOneSuccessfulCat(introspector, txName);

            // external request information
            Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);
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
            httpClient.get(httpServer.getEndPoint().getPort(), httpServer.getEndPoint().getHost(), "/")
                  .putHeader(HttpTestServer.DO_CAT, "true")
                  .handler(requestHandler(latch))
                  .end();
            latch.await();
        } finally {
            httpServer.shutdown();
        }
    }


    @Test
    public void testUnknownHost() throws Exception {
        unknownHost();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(250));

        final String txn = introspector.getTransactionNames().iterator().next();
        assertNotNull("Transaction not found", txn);

        assertEquals(1, MetricsHelper.getScopedMetricCount(txn, "External/UnknownHost/Vertx-Client/end"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/UnknownHost/Vertx-Client/end"));

        // Unknown hosts generate no external rollups
        assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/allOther"));
        assertEquals(0, MetricsHelper.getUnscopedMetricCount("External/all"));

        // Make sure exception handler is linked
        final TransactionEvent event = introspector.getTransactionEvents(txn).iterator().next();
        assertTrue(event.getAttributes().containsKey("exceptionHandler"));
    }


    @Trace(dispatcher = true)
    private void unknownHost() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();
        httpClient.get(port, "notARealHostDuderina.com", "/")
                  .exceptionHandler(exceptionHandler(latch))
                  .handler(requestHandler(null))
                  .end();
        latch.await();
    }

    private Handler<Throwable> exceptionHandler(CountDownLatch latch) {
        return error -> {
            NewRelic.addCustomParameter("exceptionHandler", "true");
            latch.countDown();
        };
    }

    private Handler<HttpClientResponse> requestHandler(CountDownLatch latch) {
        return response -> {
            NewRelic.addCustomParameter("responseHandler", "true");
            response.bodyHandler(body -> {
                NewRelic.addCustomParameter("bodyHandler", "true");
                latch.countDown();
            });
        };
    }

    public void assertExternal(String transactionName, String host) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(transactionName);
        ExternalRequest request = externalRequests.iterator().next();
        assertEquals(host, request.getHostname());
        assertEquals("Vertx-Client", request.getLibrary());
        assertEquals("end", request.getOperation());
        Collection<TransactionEvent> events = introspector.getTransactionEvents(transactionName);
        TransactionEvent event = events.iterator().next();
        assertTrue(event.getAttributes().containsKey("responseHandler"));

        assertEquals(1, MetricsHelper.getScopedMetricCount(transactionName, "External/localhost/Vertx-Client/end"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/Vertx-Client/end"));

        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(transactionName);
        assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        assertEquals(1, transactionEvent.getExternalCallCount());
        assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        // external rollups
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/localhost/all"));
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
