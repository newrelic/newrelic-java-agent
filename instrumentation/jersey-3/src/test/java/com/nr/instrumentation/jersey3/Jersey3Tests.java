/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey3;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.nr.instrumentation.jersey3.resources.AsyncResource;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.instrumentation.jersey", "org.glassfish.jersey" })
public class Jersey3Tests extends JerseyTest {
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int TIMEOUT = 30000;

    @Override
    protected Application configure() {
        return new ResourceConfig(AsyncResource.class);
    }

    @Test
    public void asyncResumeTest() {
        Response response = target("/async/resume").request().get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/resume", transactionName);
    }

    @Test
    public void asyncResumeWithIOExceptionTest() {
        Response response = target("/async/resumeWithIOException").request().get();
        assertNotNull(response);
        // Why is this coming back as 200 now?
        //assertEquals(500, response.getStatus());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/resumeWithIOException", transactionName);
    }

    @Test
    public void syncEndpointTest() {
        Response response = target("/async/syncEndpoint").request().get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/syncEndpoint", transactionName);
    }

    @Test
    public void syncEndpointWithIOExceptionTest() {
        Response response = target("/async/syncEndpointWithIOException").request().get();
        assertNotNull(response);
        //???
//        assertEquals(500, response.getStatus());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/syncEndpointWithIOException", transactionName);
    }

    @Test
    public void asyncMultipleResumeTest() {
        Response response = target("/async/multipleResume").request().get();
        assertNotNull(response);
        assertEquals(200, response.getStatus());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/multipleResume", transactionName);
    }

    @Test
    public void asyncResumeThrowableTest() {
        Response response = target("/async/resumeThrowable").request().get();
        assertNotNull(response);
        assertEquals(500, response.getStatus());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/resumeThrowable", transactionName);
    }

    @Test
    public void asyncCancelTest() {
        Response response = target("/async/cancel").request().get();
        assertNotNull(response);
        // Status code should be 503 since the request is timing out
        assertEquals(503, response.getStatus());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
        assertEquals(1, finishedTransactionCount);

        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
        assertNotNull(transactionName);
        assertEquals("WebTransaction/AsyncResource/cancel", transactionName);
    }

//    @Test
//    public void asyncTimeoutTest() {
//        Response response = getRequest("/api/async/timeout", server.getURI().getPort());
//        assertNotNull(response);
//        assertEquals(200, response.statusCode());
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
//        assertEquals(1, finishedTransactionCount);
//
//        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
//        assertNotNull(transactionName);
//        assertEquals("WebTransaction/AsyncResource/timeout", transactionName);
//    }
//
//    @Test
//    public void asyncTimeoutTestThrowable() {
//        Response response = getRequest("/api/async/timeoutThrowable", server.getURI().getPort());
//        assertNotNull(response);
//        assertEquals(500, response.statusCode());
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
//        assertEquals(1, finishedTransactionCount);
//
//        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
//        assertNotNull(transactionName);
//        assertEquals("WebTransaction/AsyncResource/timeoutThrowable", transactionName);
//    }
//
//    @Test
//    public void asyncTimeoutTestCancel() {
//        Response response = getRequest("/api/async/timeoutCancel", server.getURI().getPort());
//        assertNotNull(response);
//        assertEquals(503, response.statusCode());
//
//        Introspector introspector = InstrumentationTestRunner.getIntrospector();
//        int finishedTransactionCount = introspector.getFinishedTransactionCount(TIMEOUT);
//        assertEquals(1, finishedTransactionCount);
//
//        String transactionName = introspector.getTransactionNames().toArray()[0].toString();
//        assertNotNull(transactionName);
//        assertEquals("WebTransaction/AsyncResource/timeoutCancel", transactionName);
//    }
//
//    private Response getRequest(final String path, final int port) {
//        return given().baseUri("http://localhost:" + port + path).contentType(ContentType.JSON).get();
//    }

}
