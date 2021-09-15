/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({ Java16IncompatibleTest.class, Java17IncompatibleTest.class })
// Test that transaction always finishes
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class ExpireAndEndHandlerTest extends VertxTestBase {

    @Test
    public void testResponseOtherThread() {
        Vertx vertx = Vertx.vertx();
        try {
            Router router = Router.router(vertx);
            router.route().path("/really-async").handler(ctx -> {
                new Thread(() -> {
                    ctx.response().putHeader("content-type", "text/plain").end("Response written from separate thread");
                }).start();
            });

            HttpServer server = createServer(vertx, router);
            getRequest("/really-async", server).then().statusCode(200);

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            assertEquals(1, introspector.getFinishedTransactionCount(500));

            Map<String, TracedMetricData> metrics = getMetrics("OtherTransaction/Vertx/really-async (GET)");
            assertTrue(metrics.containsKey("com.nr.vertx.instrumentation.ExpireAndEndHandlerTest.lambda()"));
        } finally {
            vertx.close();
        }
    }

    @Test
    public void testJDBC() {
        Vertx vertx = Vertx.vertx();
        try {
            Router router = Router.router(vertx);
            JDBCClient client = JDBCClient.createShared(vertx,
                    new JsonObject().put("url", "jdbc:hsqldb:mem:test?shutdown=true").put("driver_class",
                            "org.hsqldb.jdbcDriver"));

            router.route().path("/jdbc").handler(ctx -> client.getConnection(res -> {
                if (res.failed()) {
                    ctx.response().setStatusCode(504).end();
                } else {
                    SQLConnection conn = res.result();
                    conn.queryWithParams("SELECT id, name, price, weight FROM products where id = ?",
                            new JsonArray().add(8), query -> {
                                ctx.response().setStatusCode(500).end();
                            });
                }
            }));
            HttpServer server = createServer(vertx, router);
            getRequest("/jdbc", server).then().statusCode(500);

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            assertEquals(1, introspector.getFinishedTransactionCount(500));

            Map<String, TracedMetricData> metrics = getMetrics("OtherTransaction/Vertx/jdbc (GET)");
            assertTrue(metrics.containsKey("com.nr.vertx.instrumentation.ExpireAndEndHandlerTest.lambda()"));
        } finally {
            vertx.close();
        }
    }
}
