/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionEvent;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

public class VertxTestBase {

    static final long TIMEOUT = 2000;

    static HttpServer httpServer;

    Response getRequest(HttpServer server, int port) {
        return getRequest("", server, port);
    }

    Response getRequest(String path, HttpServer server, int port) {
        return given().baseUri("http://localhost:" + port + path).contentType(ContentType.TEXT).get();
    }

    Response postRequest(String path, HttpServer server, int port) {
        return given().baseUri("http://localhost:" + port + path).contentType(ContentType.TEXT).post();
    }

    Response putRequest(String path, HttpServer server, int port) {
        return given().baseUri("http://localhost:" + port + path).contentType(ContentType.TEXT).put();
    }

    Response deleteRequest(String path, HttpServer server, int port) {
        return given().baseUri("http://localhost:" + port + path).contentType(ContentType.TEXT).delete();
    }

    Response headRequest(String path, HttpServer server, int port) {
        return given().baseUri("http://localhost:" + port + path).contentType(ContentType.TEXT).head();
    }

    Map<String, Object> getAttributesForTransaction(String expectedTxnName) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        Collection<TransactionEvent> txEvents = introspector.getTransactionEvents(expectedTxnName);
        TransactionEvent event = txEvents.iterator().next();
        return event.getAttributes();
    }

    Map<String, TracedMetricData> getMetrics(String txnName) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(TIMEOUT));
        return InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txnName);
    }

    HttpServer createServer(Vertx vertx, Router router, int port) {


        final CountDownLatch serverReady = new CountDownLatch(1);

        final HttpServer server = vertx.createHttpServer().requestHandler(router::accept).listen(port,
                listenHandler -> {
                    serverReady.countDown();
                });

        try {
            serverReady.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return server;
    }

    public int getAvailablePort() {
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
