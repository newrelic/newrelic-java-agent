/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;

import static com.nr.vertx.test.handlers.SimpleHandlers.createHandler;
import static com.nr.vertx.test.handlers.SimpleHandlers.createLambdaHandler;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;

@Category({ Java16IncompatibleTest.class, Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class RoutingTest extends VertxTestBase{

    @Test
    public void testExactPath() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route route = router.route().path("/swiss-cheese-knife");
            route.handler(createHandler("CheeseHandler", true));
            HttpServer server = createServer(vertx, router);

            getRequest("/swiss-cheese-knife?medium=true&looknomgi=omgno", server)
                    .then().body(containsString("CheeseHandler sent response"));

            Map<String, Object> attributes = getAttributesForTransaction("OtherTransaction/Vertx/swiss-cheese-knife (GET)");
            assertNotNull(attributes.get("CheeseHandler"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testRegexBeginsWith() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route route = router.route().path("/best/first/place/to*");
            route.handler(createHandler("BestFirstPlace", true));
            HttpServer server = createServer(vertx, router);

            Map<String, Object> attributes;

            getRequest("/best/first/place/to/understand/your/", server)
                    .then().body(containsString("BestFirstPlace sent response"));

            attributes = getAttributesForTransaction("OtherTransaction/Vertx/best/first/place/to (GET)");
            assertNotNull(attributes.get("BestFirstPlace"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/best/first/place/to/understand/your/digital", server)
                    .then().body(containsString("BestFirstPlace sent response"));

            attributes = getAttributesForTransaction("OtherTransaction/Vertx/best/first/place/to (GET)");
            assertNotNull(attributes.get("BestFirstPlace"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/best/first/place/to/understand/your/digital/biscuit", server)
                    .then().body(containsString("BestFirstPlace sent response"));

            attributes = getAttributesForTransaction("OtherTransaction/Vertx/best/first/place/to (GET)");
            assertNotNull(attributes.get("BestFirstPlace"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/best/first/place/to/understand/your/digital/biscuit?nutella=yes", server)
                    .then().body(containsString("BestFirstPlace sent response"));

            attributes = getAttributesForTransaction("OtherTransaction/Vertx/best/first/place/to (GET)");
            assertNotNull(attributes.get("BestFirstPlace"));
        } finally {
            vertx.close();
        }
    }


    @Test
    public void testCaptureParams() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route route = router.route().path("/coffee/:origin/:roast/");
            route.handler(createLambdaHandler("CoffeeHandler", true));

            HttpServer server = createServer(vertx, router);

            Map<String, Object> attributes;

            getRequest("/coffee/ghana/medium/", server).then().body(containsString("CoffeeHandler sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/coffee/:origin/:roast/ (GET)");
            assertNotNull(attributes.get("CoffeeHandler"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/coffee/gresham/french/?nomgi=wat", server).then().body(containsString("CoffeeHandler sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/coffee/:origin/:roast/ (GET)");
            assertNotNull(attributes.get("CoffeeHandler"));
            InstrumentationTestRunner.getIntrospector().clear();
        } finally {
            vertx.close();
        }

    }


    @Test
    public void testRouteRegex() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route route = router.route().pathRegex(".*agent");
            route.handler(createLambdaHandler("Agent", true));
            HttpServer server = createServer(vertx, router);

            Map<String, Object> attributes;

            getRequest("/newrelic/java/agent", server).then().body(containsString("Agent sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/.*agent (GET)");
            assertNotNull(attributes.get("Agent"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/newrelic/agent", server).then().body(containsString("Agent sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/.*agent (GET)");
            assertNotNull(attributes.get("Agent"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/agent", server).then().body(containsString("Agent sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/.*agent (GET)");
            assertNotNull(attributes.get("Agent"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testRouteRegexCaptureParams() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route route = router.route().pathRegex("\\/([^\\/]+)\\/([^\\/]+)");
            route.blockingHandler(createLambdaHandler("TwoGroups", true));
            HttpServer server = createServer(vertx, router);

            Map<String, Object> attributes;

            getRequest("/tools/drill123", server).then().body(containsString("TwoGroups sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/\\/([^\\/]+)\\/([^\\/]+) (GET)");
            assertNotNull(attributes.get("TwoGroups"));
            InstrumentationTestRunner.getIntrospector().clear();

            getRequest("/first/second?query=parameters&favorite=red", server).then().body(containsString("TwoGroups sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/\\/([^\\/]+)\\/([^\\/]+) (GET)");
            assertNotNull(attributes.get("TwoGroups"));
            InstrumentationTestRunner.getIntrospector().clear();
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testRouteByHttpMethod() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route viewProducts = router.route(HttpMethod.GET, "/products");
            viewProducts.handler(createLambdaHandler("ProductHandlerGet", true));

            Route uploadProduct = router.route(HttpMethod.POST, "/products/upload");
            uploadProduct.handler(createLambdaHandler("ProductHandlerPost", true));

            HttpServer server = createServer(vertx, router);

            Map<String, Object> attributes;

            getRequest("/products", server).then().body(containsString("ProductHandlerGet sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/products (GET)");
            assertNotNull(attributes.get("ProductHandlerGet"));
            InstrumentationTestRunner.getIntrospector().clear();

            postRequest("/products/upload", server).then().body(containsString("ProductHandlerPost sent response"));
            attributes = getAttributesForTransaction("OtherTransaction/Vertx/products/upload (POST)");
            assertNotNull(attributes.get("ProductHandlerPost"));
            InstrumentationTestRunner.getIntrospector().clear();
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testRouteByMimeType() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route textPlain = router.route().consumes("text/plain");
            textPlain.handler(createLambdaHandler("TextPlainHandler", true));
            HttpServer server = createServer(vertx, router);

            getRequest(server).then().body(containsString("TextPlainHandler"));
            final Map<String, Object> attributes = getAttributesForTransaction("OtherTransaction/Vertx/UnnamedPath (GET)");
            assertNotNull(attributes.get("TextPlainHandler"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testRouteCombinedCriteria() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);
            Route orders = router.route(HttpMethod.POST, "/myapi/orders")
                    .consumes("text/plain")
                    .produces("text/plain");
            orders.blockingHandler(createLambdaHandler("OrdersHandler", true));
            HttpServer server = createServer(vertx, router);

            postRequest("/myapi/orders", server).then().body(containsString("OrdersHandler sent response"));
            final Map<String, Object> attributes = getAttributesForTransaction("OtherTransaction/Vertx/myapi/orders (POST)");
            assertNotNull(attributes.get("OrdersHandler"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testSubRouters() {
        Vertx vertx = Vertx.vertx();
        try {

            Router mainRouter = Router.router(vertx);
            mainRouter.route().blockingHandler(createLambdaHandler("MainHandler", false), false);

            Router helloRouter = Router.router(vertx);
            helloRouter.route("/hello").handler(createLambdaHandler("HelloHandler", true));

            mainRouter.mountSubRouter("/api", helloRouter);

            HttpServer server = createServer(vertx, mainRouter);
            getRequest("/api/hello", server).then().body(containsString("HelloHandler"));

            final Map<String, Object> attributes = getAttributesForTransaction("OtherTransaction/Vertx/api/hello (GET)");
            assertNotNull(attributes.get("MainHandler"));
            assertNotNull(attributes.get("HelloHandler"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testRerouteHandler() {
        Vertx vertx = Vertx.vertx();
        try {

            Router router = Router.router(vertx);

            router.route().handler(BodyHandler.create().setBodyLimit(30000));

            router.get("/some/path").handler(routingContext -> {
                routingContext.put("foo", "bar");
                NewRelic.addCustomParameter("SomePath1", "SomePath1");
                routingContext.next();
            });
            router.get("/some/path").handler(routingContext -> {
                NewRelic.addCustomParameter("SomePath2", "SomePath2");
                routingContext.reroute("/some/path/B");
            });
            router.get("/some/path/B").handler(routingContext -> {
                NewRelic.addCustomParameter("SomePath3", "SomePath3");
                routingContext.response().end("Reroute: " + routingContext.get("foo"));
            });
            HttpServer server = createServer(vertx, router);

            getRequest("/some/path", server).then().body(CoreMatchers.containsString("Reroute: bar"));

            Map<String, Object> attributes = getAttributesForTransaction("OtherTransaction/Vertx/some/path/some/path/some/path/B (GET)");

            Assert.assertNotNull(attributes.get("SomePath1"));
            Assert.assertNotNull(attributes.get("SomePath2"));
            Assert.assertNotNull(attributes.get("SomePath3"));
        } finally {
            vertx.close();
        }
    }

}
