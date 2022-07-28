/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jayway.restassured.RestAssured.given;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.instrumentation.jersey", "org.glassfish.jersey" })
public class JerseyTests {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int TIMEOUT = 30000;

    private static Server server;

    @BeforeClass
    public static void setUp() {
        server = new Server(8089);

        ServletContextHandler servletContextHandler = new ServletContextHandler(NO_SESSIONS);

        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        ServletHolder servletHolder = servletContextHandler.addServlet(String.valueOf(ServletContainer.class), "/api/*");
        servletHolder.setInitOrder(0);
        servletHolder.setInitParameter(
                "jersey.config.server.provider.packages",
                "com.nr.instrumentation.jersey.resources"
        );

        try {
            server.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            server.stop();
        } catch (Exception e) {
            server.destroy();
        }
    }

    @Test
    public void asyncResumeTest() {
        Response response = getRequest("/api/async/resume", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(200, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/resume", transactionName);
    }

    @Test
    public void asyncResumeWithIOExceptionTest() {
        Response response = getRequest("/api/async/resumeWithIOException", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(500, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/resumeWithIOException", transactionName);
    }

    @Test
    public void syncEndpointTest() {
        Response response = getRequest("/api/async/syncEndpoint", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(200, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/syncEndpoint", transactionName);
    }

    @Test
    public void syncEndpointWithIOExceptionTest() {
        Response response = getRequest("/api/async/syncEndpointWithIOException", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(500, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/syncEndpointWithIOException", transactionName);
    }

    @Test
    public void asyncMultipleResumeTest() {
        Response response = getRequest("/api/async/multipleResume", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(200, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/multipleResume", transactionName);
    }

    @Test
    public void asyncResumeThrowableTest() {
        Response response = getRequest("/api/async/resumeThrowable", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(500, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/resumeThrowable", transactionName);
    }

    @Test
    public void asyncCancelTest() {
        Response response = getRequest("/api/async/cancel", server.getURI().getPort());
        assertNotNull(response);
        // Status code should be 503 since the request is timing out
        assertEquals(503, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/cancel", transactionName);
    }

    @Test
    public void asyncTimeoutTest() {
        Response response = getRequest("/api/async/timeout", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(200, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/timeout", transactionName);
    }

    @Test
    public void asyncTimeoutTestThrowable() {
        Response response = getRequest("/api/async/timeoutThrowable", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(500, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/timeoutThrowable", transactionName);
    }

    @Test
    public void asyncTimeoutTestCancel() {
        Response response = getRequest("/api/async/timeoutCancel", server.getURI().getPort());
        assertNotNull(response);
        assertEquals(503, response.statusCode());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/timeoutCancel", transactionName);
    }

    private Response getRequest(final String path, final int port) {
        return given().baseUri("http://localhost:" + port + path).contentType(ContentType.JSON).get();
    }

}
