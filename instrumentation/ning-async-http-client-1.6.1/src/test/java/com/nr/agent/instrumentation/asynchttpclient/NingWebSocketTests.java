/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.asynchttpclient", "com.ning" })
public class NingWebSocketTests {

    private static EchoServer server;

    @BeforeClass
    public static void beforeClass() throws IOException {
        server = new EchoServer(9192);
        server.start();
    }

    @AfterClass
    public static void afterClass() {
        server.stop();
    }

    @Test
    public void testWebSocket() throws Exception {
        // we don't support websockets, but we don't want to break them
        // so we make a websocket request and check that no external requests were traced
        makeWebSocketRequest();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getTransactionNames().size());

        String txName = introspector.getTransactionNames().iterator().next();
        for(String metricName : introspector.getMetricsForTransaction(txName).keySet()) {
            assertFalse(metricName.startsWith("External/"));
        }

        assertNull(introspector.getExternalRequests(txName));
    }

    @Trace(dispatcher = true)
    public static void makeWebSocketRequest() throws Exception {
        WebSocket ws = null;
        try (AsyncHttpClient client = new AsyncHttpClient()) {
            final CountDownLatch messageReceivedSignal = new CountDownLatch(1);
            final String hello = "Hello Echo WebSocket!";
            ws = client.prepareGet("ws://localhost:9192").execute(
                    new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketTextListener() {
                        @Override
                        public void onOpen(WebSocket websocket) {
                            websocket.sendMessage(hello);
                        }

                        @Override
                        public void onClose(WebSocket websocket) {
                        }

                        @Override
                        public void onError(Throwable t) {
                        }

                        @Override
                        public void onMessage(String message) {
                            assertEquals(hello, message);
                            messageReceivedSignal.countDown();
                        }
                    }).build()).get();
            messageReceivedSignal.await();
        } finally {
            if (ws != null && ws.isOpen()) {
                ws.close();
            }
        }
    }

}
