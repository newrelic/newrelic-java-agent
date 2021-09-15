/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.nr.vertx.test.handlers.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;

import static com.nr.vertx.test.handlers.SimpleHandlers.createHandler;
import static com.nr.vertx.test.handlers.SimpleHandlers.createLambdaHandler;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;

@Category({ Java16IncompatibleTest.class, Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx", "com.nr.vert" })
public class VertxHandlersTest extends VertxTestBase {

    public static final String UNNAMED_PATH = "OtherTransaction/Vertx/UnnamedPath (GET)";

    @Test
    public void testSerialRequestHandlers() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            router.route().handler(createLambdaHandler("FirstHandler", false));
            router.route().handler(createHandler("SecondHandler", false));
            router.route().handler(createHandler("ThirdHandler", true));
            HttpServer server = createServer(vertx, router);

            getRequest(server).then().body(containsString("ThirdHandler sent response"));

            Map<String, Object> attributes = getAttributesForTransaction(UNNAMED_PATH);

            assertNotNull(attributes.get("FirstHandler"));
            assertNotNull(attributes.get("SecondHandler"));
            assertNotNull(attributes.get("ThirdHandler"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testBlockingAsyncHandlers() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            router.route().blockingHandler(createLambdaHandler("FirstHandler", false));
            router.route().blockingHandler(createHandler("FirstHandlerParallel", false), false);
            router.route().blockingHandler(createLambdaHandler("SecondHandlerParallel", false), false);
            router.route().blockingHandler(createLambdaHandler("ThirdHandlerParallel", false), false);
            router.route().handler(createLambdaHandler("SecondHandler", false));
            router.route().blockingHandler(createLambdaHandler("ThirdHandler", true));
            HttpServer server = createServer(vertx, router);

            getRequest(server).then().body(containsString("ThirdHandler sent response"));

            Map<String, Object> attributes = getAttributesForTransaction(UNNAMED_PATH);

            assertNotNull(attributes.get("FirstHandler"));
            assertNotNull(attributes.get("FirstHandlerParallel"));
            assertNotNull(attributes.get("SecondHandlerParallel"));
            assertNotNull(attributes.get("ThirdHandlerParallel"));
            assertNotNull(attributes.get("SecondHandler"));
            assertNotNull(attributes.get("ThirdHandler"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testNoErrorHandlerFail() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            router.route().blockingHandler(createLambdaHandler("FirstHandler", false));
            router.route().blockingHandler(ErrorHandler.createHandlerWithStatusCode("HandlerWithStatusCode", 403));
            HttpServer server = createServer(vertx, router);

            getRequest(server).then().statusCode(403);

            Map<String, Object> attributes = getAttributesForTransaction(UNNAMED_PATH);

            assertNotNull(attributes.get("FirstHandler"));
            assertNotNull(attributes.get("HandlerWithStatusCode"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testNoErrorHandlerFailThrowable() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            router.route().blockingHandler(createLambdaHandler("FirstHandler", false));
            router.route().blockingHandler(
                    ErrorHandler.createHandlerWithException("HandlerWithException", new RuntimeException("not good")));
            HttpServer server = createServer(vertx, router);

            getRequest(server).then().statusCode(500);

            Map<String, Object> attributes = getAttributesForTransaction(UNNAMED_PATH);

            assertNotNull(attributes.get("FirstHandler"));
            assertNotNull(attributes.get("HandlerWithException"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testFailureHandler() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            router.route().blockingHandler(
                    ErrorHandler.createHandlerWithException("StatusCodeFail", new RuntimeException("not good")));
            router.route().failureHandler(ErrorHandler.createFailureHandler("FailureHandler"));
            HttpServer server = createServer(vertx, router);

            getRequest(server).then().statusCode(530).body(containsString("It's cold here!"));

            Map<String, Object> attributes = getAttributesForTransaction(UNNAMED_PATH);

            assertNotNull(attributes.get("StatusCodeFail"));
            assertNotNull(attributes.get("FailureHandler"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testFailureHandlerException() {
        // What if the error handler fails? Test that the transaction finishes

        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            router.route().blockingHandler(
                    ErrorHandler.createHandlerWithException("ThrowableFail", new RuntimeException("not good")));
            router.route().failureHandler(ErrorHandler.createFailureHandlerException("FailureHandler"));
            HttpServer server = createServer(vertx, router);

            getRequest(server).then().statusCode(500);

            Map<String, Object> attributes = getAttributesForTransaction(UNNAMED_PATH);
            assertNotNull(attributes.get("ThrowableFail"));
            assertNotNull(attributes.get("FailureHandler"));
        } finally {
            vertx.close();
        }
    }

}
