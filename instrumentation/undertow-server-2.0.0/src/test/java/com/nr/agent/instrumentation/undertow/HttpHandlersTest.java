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
import io.undertow.Undertow;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.PathTemplatePredicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;

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
    public void nonInstrumentedHandler_createsConstantTxnName() throws IOException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        startServerWithHandler(exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Hello, Undertow!");
        });

        exerciseEndpoint("http://localhost:" + port + "/");

        waitForTxnToFinish(introspector, 1);
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Undertow/{connectors_placeholder_name}/ (GET)"));
    }

    @Test
    public void parameterizedRouteHandler_namesTxnCorrectly() throws IOException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        RoutingHandler routingHandler = new RoutingHandler();

        routingHandler.get("/greet/{name}", exchange -> {
            String name = exchange.getQueryParameters().get("name").getFirst();
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Hello, " + name + "!");
        });

        startServerWithHandler(routingHandler);

        exerciseEndpoint("http://localhost:" + port + "/greet/claptrap");

        waitForTxnToFinish(introspector, 1);
        assertTrue(introspector.getTransactionNames().contains("WebTransaction/Undertow/greet/{name} (GET)"));
    }

    @Test
    public void staticRouteHandler_namesTxnCorrectly() throws IOException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        RoutingHandler routingHandler = new RoutingHandler();

        routingHandler.get("/static/content", exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Static content");
        });

        startServerWithHandler(routingHandler);

        exerciseEndpoint("http://localhost:" + port + "/static/content");

        waitForTxnToFinish(introspector, 1);
        assertTrue(introspector.getTransactionNames().contains("WebTransaction/Undertow/static/content (GET)"));
    }

    @Test
    public void pathTemplateHandlerHandler_namesTxnCorrectly() throws IOException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        PathTemplateHandler pathTemplateHandler = new PathTemplateHandler();

        pathTemplateHandler.add("/greet/{name}", exchange -> {
            PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
            String name = pathMatch.getParameters().get("name");
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Hello " + name + "!");
        });

        startServerWithHandler(pathTemplateHandler);

        exerciseEndpoint("http://localhost:" + port + "/greet/claptrap");

        waitForTxnToFinish(introspector, 1);
        assertTrue(introspector.getTransactionNames().contains("WebTransaction/Undertow/greet/{name} (GET)"));
    }

    @Test
    public void PathTemplatePredicateHandler_namesTxnCorrectly() throws IOException {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        HttpHandler matchedPathHandler = exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Path matched!");
        };
        HttpHandler unmatchedPathHandler = exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Path not matched.");
        };
        PredicateHandler predicateHandler = new PredicateHandler(
                new PathTemplatePredicate("/greet/{name}", ExchangeAttributes.relativePath()),
                matchedPathHandler,
                unmatchedPathHandler
        );

        startServerWithHandler(predicateHandler);

        exerciseEndpoint("http://localhost:" + port + "/greet/claptrap");

        waitForTxnToFinish(introspector, 1);
        assertTrue(introspector.getTransactionNames().contains("WebTransaction/Undertow/greet/{name} (GET)"));
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

        return response.toString();
    }

    private void waitForTxnToFinish(Introspector introspector, int expectedTxnCount) {
        long expireTime = System.currentTimeMillis() + 30000;

        while (System.currentTimeMillis() < expireTime && introspector.getTransactionNames().size() != expectedTxnCount) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {}
        }

        if (introspector.getTransactionNames().size() != expectedTxnCount) {
            System.out.println("Transaction never finished -- test will fail");
        }
    }
}
