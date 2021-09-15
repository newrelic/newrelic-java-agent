/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.nr.vertx.test.handlers.ProductHandlers;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertTrue;

@Category({ Java16IncompatibleTest.class, Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class HandlerTracingTest extends VertxTestBase {

    @Test
    public void testLambdaNames() {
        Vertx vertx = Vertx.vertx();
        try {
            Router router = Router.router(vertx);
            router.route().path("/products").handler(ProductHandlers::getAllProducts);
            router.route().path("/product/:pid").handler(ProductHandlers.getProductHandler());
            HttpServer server = createServer(vertx, router);
            getRequest("/products/", server).then().statusCode(200);
            Map<String, TracedMetricData> metrics = getMetrics("OtherTransaction/Vertx/products (GET)");
            assertTrue(metrics.containsKey("com.nr.vertx.instrumentation.HandlerTracingTest.lambda()"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/product/milk", server).then().statusCode(200);
            metrics = getMetrics("OtherTransaction/Vertx/product/:pid (GET)");
            assertTrue(metrics.containsKey("com.nr.vertx.test.handlers.ProductHandlers.lambda()"));
            InstrumentationTestRunner.getIntrospector().clear();
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testLambdaNamesBlocking() {
        Vertx vertx = Vertx.vertx();
        try {
            Router router = Router.router(vertx);
            router.route().path("/products").blockingHandler(ProductHandlers::getAllProducts);
            router.route().path("/product/:pid").blockingHandler(ProductHandlers.getProductHandler());
            HttpServer server = createServer(vertx, router);

            getRequest("/products/", server).then().statusCode(200);
            Map<String, TracedMetricData> metrics = getMetrics("OtherTransaction/Vertx/products (GET)");
            assertTrue(metrics.containsKey("com.nr.vertx.instrumentation.HandlerTracingTest.lambda()"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/product/milk", server).then().statusCode(200);
            metrics = getMetrics("OtherTransaction/Vertx/product/:pid (GET)");
            assertTrue(metrics.containsKey("com.nr.vertx.test.handlers.ProductHandlers.lambda()"));
            InstrumentationTestRunner.getIntrospector().clear();
        } finally {
            vertx.close();
        }
    }

}
