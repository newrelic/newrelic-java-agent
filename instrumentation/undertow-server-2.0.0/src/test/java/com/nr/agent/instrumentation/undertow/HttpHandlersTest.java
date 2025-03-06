/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.undertow;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"io.undertow"})
public class HttpHandlersTest {
    private Undertow server;
    private int port;

    @Before
    public void setup() {
        port = InstrumentationTestRunner.getIntrospector().getRandomPort();
    }

    @After
    public void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void nonInstrumentedHandler_createsConstantTxnName() throws IOException, InterruptedException {
        startServerWithHandler(exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Hello, Undertow!");
        });

        TestClient client = new TestClient("http://localhost:" + port + "/");
        Thread t = new Thread(client);
        t.start();
        t.join();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        introspector.getTransactionNames();
    }

    private void startServerWithHandler(HttpHandler handler) {
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(handler)
                .build();
        server.start();
    }

    private String exerciseEndpoint(String endpoint) throws IOException {
        StringBuilder response = new StringBuilder();

        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Get the response
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        connection.disconnect();

        return response.toString();
    }

    private static class TestClient implements Runnable {
        String endpoint;
        String response;

        public TestClient(String endpoint) {
            this.endpoint = endpoint;
        }
        @Override
        public void run() {
            StringBuilder response = new StringBuilder();

            try {
                URL url = new URL(endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                connection.disconnect();

                this.response = response.toString();
            } catch(Exception ignored) {
            }
        }
    }
}
