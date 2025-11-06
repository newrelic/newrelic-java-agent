/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.DataStoreRequest;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.HttpTestServer;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerLocator;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class VertxPostgresSqlClientTest {

    private static int port;
    private static Future<HttpServer> server;
    private static Vertx vertx;
    private static SqlClient sqlClient;

    private static PostgreSQLContainer<?> postgres;

    @BeforeClass
    public static void beforeClass() throws InterruptedException {

        postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();
        port = getAvailablePort();
        vertx = Vertx.vertx();

        final Router router = Router.router(vertx);
        router.get("/fetch").handler(VertxPostgresSqlClientTest::handleFetchRequest);
        router.get("/insert").handler(VertxPostgresSqlClientTest::handleInsertRequest);

        server = vertx.createHttpServer().requestHandler(router).listen(port);

        configureSqlClient();
        initDbTable();
    }

    @AfterClass
    public static void afterClass() {
        server.result().close();
        vertx.close();
        postgres.stop();
    }

    @Test
    public void testPgSqlClientFetch() throws InterruptedException {
        doPgFetchTransaction();
        // Wait for transaction to finish
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxPostgresSqlClientTest/performSimpleQuery"));

        ArrayList<TransactionTrace> traces = new ArrayList<>(introspector.getTransactionTracesForTransaction("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxPostgresSqlClientTest/performSimpleQuery"));
        ArrayList<TraceSegment> segments = new ArrayList<>(traces.get(0).getInitialTraceSegment().getChildren());

        assertEquals(2, segments.size());
        assertEquals("Java/io.vertx.sqlclient.impl.PoolImpl/query", segments.get(0).getName());
        assertEquals("Java/io.vertx.sqlclient.impl.SqlClientBase$QueryImpl/execute", segments.get(1).getName());

        ArrayList<DataStoreRequest> datastores = new ArrayList<>(introspector.getDataStores("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxPostgresSqlClientTest/performSimpleQuery"));
        assertEquals(1, datastores.get(0).getCount());
        assertEquals("test", datastores.get(0).getTable());
        assertEquals("SELECT", datastores.get(0).getOperation());
        assertEquals("Postgres", datastores.get(0).getDatastore());
    }

    public void doPgFetchTransaction() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();

        httpClient.request(HttpMethod.GET, port,"localhost", "/fetch", reqAsyncResult -> {
            if (reqAsyncResult.succeeded()) {   //Request object successfully created
                HttpClientRequest request = reqAsyncResult.result();
                request.send(respAsyncResult -> {   //Sending the request
                    if (respAsyncResult.succeeded()) {
                        HttpClientResponse response = respAsyncResult.result();
                        latch.countDown();
                        response.body(respBufferAsyncResult -> {  //Retrieve response
                            if (respBufferAsyncResult.succeeded()) {
                                System.out.println(respBufferAsyncResult.result().toString());
                            } else {
                                // Handle server error, for example, connection closed
                            }
                        });
                    } else {
                        // Handle server error, for example, connection closed
                    }
                });
            } else {
                // Connection error, for example, invalid server or invalid SSL certificate
            }
        });
        latch.await();
    }

    @Test
    public void testPgSqlClientInsert() throws InterruptedException {
        doPgInsertTransaction();
        // Wait for transaction to finish
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxPostgresSqlClientTest/performInsertQuery"));

        ArrayList<TransactionTrace> traces = new ArrayList<>(introspector.getTransactionTracesForTransaction("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxPostgresSqlClientTest/performInsertQuery"));
        ArrayList<TraceSegment> segments = new ArrayList<>(traces.get(0).getInitialTraceSegment().getChildren());

        assertEquals(2, segments.size());
        assertEquals("Java/io.vertx.sqlclient.impl.PoolImpl/query", segments.get(0).getName());
        assertEquals("Java/io.vertx.sqlclient.impl.SqlClientBase$QueryImpl/execute", segments.get(1).getName());

        ArrayList<DataStoreRequest> datastores = new ArrayList<>(introspector.getDataStores("OtherTransaction/Custom/com.nr.vertx.instrumentation.VertxPostgresSqlClientTest/performInsertQuery"));
        assertEquals(1, datastores.get(0).getCount());
        assertEquals("test", datastores.get(0).getTable());
        assertEquals("INSERT", datastores.get(0).getOperation());
        assertEquals("Postgres", datastores.get(0).getDatastore());
    }

    public void doPgInsertTransaction() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient httpClient = vertx.createHttpClient();

        httpClient.request(HttpMethod.GET, port,"localhost", "/insert", reqAsyncResult -> {
            if (reqAsyncResult.succeeded()) {   //Request object successfully created
                HttpClientRequest request = reqAsyncResult.result();
                request.send(respAsyncResult -> {   //Sending the request
                    if (respAsyncResult.succeeded()) {
                        HttpClientResponse response = respAsyncResult.result();
                        latch.countDown();
                        response.body(respBufferAsyncResult -> {  //Retrieve response
                            if (respBufferAsyncResult.succeeded()) {
                                System.out.println(respBufferAsyncResult.result().toString());
                            } else {
                                // Handle server error, for example, connection closed
                            }
                        });
                    } else {
                        // Handle server error, for example, connection closed
                    }
                });
            } else {
                // Connection error, for example, invalid server or invalid SSL certificate
            }
        });
        latch.await();
    }

    @Trace
    private static void handleFetchRequest(RoutingContext routingContext) {
        performSimpleQuery().onComplete(ar -> {
            if (ar.succeeded()) {
                StringBuilder payload = new StringBuilder();
                for (Row r : ar.result()) {
                    payload.append(r.getLong("id")).append(" ").append(r.getString("name")).append("\n");
                }
                routingContext.response().end(payload.toString());
            } else {
                routingContext.response().end("Failed to fetch data from Postgres");
            }
        });
    }

    @Trace(dispatcher = true)
    public static Future<RowSet<Row>> performSimpleQuery() {
        return sqlClient.query("select * from test").execute();
    }

    @Trace
    private static void handleInsertRequest(RoutingContext routingContext) {
        performInsertQuery().onComplete(ar -> {
            if (ar.succeeded()) {
                routingContext.response().end("row inserted");
            } else {
                routingContext.response().end("Failed to insert data into table");
            }
        });
    }

    @Trace(dispatcher = true)
    public static Future<RowSet<Row>> performInsertQuery() {
        return sqlClient.query("insert into test (id, name) values (1, 'pandora')").execute();
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

    private static void initDbTable() throws InterruptedException {
        System.out.println("Creating table...");
        final CountDownLatch createTableLatch = new CountDownLatch(1);
        sqlClient.query("create table test (name character varying(255), id bigint NOT NULL)")
                .execute()
                .onComplete(result -> createTableLatch.countDown());

        createTableLatch.await();
        System.out.println("Table created");

        System.out.println("Inserting rows...");
        final CountDownLatch insertRowsLatch = new CountDownLatch(3);
        sqlClient.query("insert into test (id, name) values (1, 'claptrap')").execute().onComplete(result -> insertRowsLatch.countDown());
        sqlClient.query("insert into test (id, name) values (2, 'krieg')").execute().onComplete(result -> insertRowsLatch.countDown());
        sqlClient.query("insert into test (id, name) values (3, 'lilith')").execute().onComplete(result -> insertRowsLatch.countDown());
        insertRowsLatch.await();
        System.out.println("All rows inserted");
    }

    private static void configureSqlClient() {
        System.out.println("Configuring SqlClient...");
        PgConnectOptions options = new PgConnectOptions()
                .setPort(postgres.getMappedPort(5432))
                .setHost(postgres.getHost())
                .setDatabase(postgres.getDatabaseName())
                .setUser(postgres.getUsername())
                .setPassword(postgres.getPassword());

        sqlClient = Pool.pool(vertx, options, new PoolOptions().setMaxSize(4));
        System.out.println("SqlClient configured");
    }
}
