/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketTextListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.asynchttpclient", "org.asynchttpclient" })
public class AsyncHttpClientWebSocketTests {

    private static EchoServer server;

    @BeforeClass
    public static void beforeClass() throws IOException {
        server = new EchoServer(9191);
        server.start();
    }

    @AfterClass
    public static void afterClass() {
        server.stop();
    }

    @Test
    public void testWebSocket() {
        // we don't support websockets, but we don't want to break them
        // so we make a websocket request and check that no external requests were traced
        makeWebSocketRequest();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getTransactionNames().size());

        String txName = introspector.getTransactionNames().iterator().next();
        for(String metricName : introspector.getMetricsForTransaction(txName).keySet()) {
            assertTrue(!metricName.startsWith("External/"));
        }

        assertEquals(null, introspector.getExternalRequests(txName));
    }

    @Trace(dispatcher = true)
    public static void makeWebSocketRequest() {
        DefaultAsyncHttpClient client = new DefaultAsyncHttpClient();
        final CountDownLatch messageReceivedSignal = new CountDownLatch(1);
        WebSocket ws = null;
        try {
            final String hello = "Hello Echo WebSocket!";
            ws = client.prepareGet("ws://localhost:9191").execute(
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
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            if(ws != null && ws.isOpen()) {
                try {
                    ws.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            client.close();
        }
    }

}
