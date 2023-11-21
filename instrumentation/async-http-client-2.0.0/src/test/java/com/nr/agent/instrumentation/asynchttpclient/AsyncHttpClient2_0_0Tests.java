/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import com.newrelic.agent.introspec.*;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.*;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({ Java11IncompatibleTest.class, Java17IncompatibleTest.class, Java21IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.nr.agent.instrumentation.asynchttpclient", "org.asynchttpclient"})
public class AsyncHttpClient2_0_0Tests {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void testCat() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        URI endpoint = server.getEndPoint();
        int status = makeAsyncRequest(endpoint.toURL().toString());
        assertEquals(200, status);

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.asynchttpclient.AsyncHttpClient2_0_0Tests/makeAsyncRequest";
        assertEquals(2, introspector.getFinishedTransactionCount());
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + endpoint.getHost() + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.asynchttpclient.AsyncHttpClient2_0_0Tests/makeAsyncRequest"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("ExternalTransaction/" + endpoint.getHost() + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + endpoint.getHost() + "/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));

        // events
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
        assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        assertEquals(1, transactionEvent.getExternalCallCount());
        assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        CatHelper.verifyOneSuccessfulCat(introspector, txName);

        // external request information
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);
        assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        assertEquals(1, externalRequest.getCount());
        assertEquals(endpoint.getHost(), externalRequest.getHostname());
        assertEquals(Integer.valueOf(200), externalRequest.getStatusCode());
        assertEquals("OK", externalRequest.getStatusText());

    }

    @Test
    public void testSuccess() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        URI endpoint = server.getEndPoint();

        String host = endpoint.getHost();
        String url = endpoint.toURL().toExternalForm();
        int status = makeAsyncRequest(url + "?no-transaction=true");
        assertEquals(200, status);

        // transaction
        assertEquals(1, introspector.getFinishedTransactionCount());
        String txName = introspector.getTransactionNames().iterator().next();

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/" + host
                + "/AsyncHttpClient/onCompleted"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/AsyncHttpClient/onCompleted"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));

        // events
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
        assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        assertEquals(1, transactionEvent.getExternalCallCount());
        assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        // traces
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txName);
        assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        for (TraceSegment segment : trace.getInitialTraceSegment().getChildren()) {
            Map<String, Object> attributes = segment.getTracerAttributes();
            if (attributes.get("async_context").equals("External")) {
                assertEquals("", segment.getMethodName());
                assertEquals("External Request", segment.getClassName());
                assertEquals("External/" + host + "/AsyncHttpClient/onCompleted", segment.getName());
                assertEquals(url, segment.getUri());
                assertEquals(1, segment.getCallCount());
            }
        }

        // external request information
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);
        assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        assertEquals(1, externalRequest.getCount());
        assertEquals(host, externalRequest.getHostname());
        assertEquals("AsyncHttpClient", externalRequest.getLibrary());
        assertEquals("onCompleted", externalRequest.getOperation());
        assertEquals(Integer.valueOf(200), externalRequest.getStatusCode());
        assertEquals("OK", externalRequest.getStatusText());
    }

    @Test
    public void testError() {
        String host = "www.thiswebsitedoesntexistreallyatallipromise.com";
        String url = "http://" + host + "/";
        int status = makeAsyncRequest(url);
        assertEquals(-1, status);

        // transaction
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        String txName = introspector.getTransactionNames().iterator().next();

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/" + host + "/AsyncHttpClient/onThrowable"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/AsyncHttpClient/onThrowable"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));

        // events
        Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
        assertEquals(1, transactionEvents.size());
        TransactionEvent transactionEvent = transactionEvents.iterator().next();
        assertEquals(1, transactionEvent.getExternalCallCount());
        assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        // traces
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txName);
        assertEquals(1, traces.size());
        TransactionTrace trace = traces.iterator().next();
        for (TraceSegment segment : trace.getInitialTraceSegment().getChildren()) {
            Map<String, Object> attributes = segment.getTracerAttributes();
            if ("External".equals(attributes.get("async_context"))) {
                assertEquals("", segment.getMethodName());
                assertEquals("External Request", segment.getClassName());
                assertEquals("External/" + host + "/AsyncHttpClient/onThrowable", segment.getName());
                assertEquals(url, segment.getUri());
                assertEquals(1, segment.getCallCount());
            }
        }

        // external request information
        Collection<ExternalRequest> externalRequests = introspector.getExternalRequests(txName);
        assertEquals(1, externalRequests.size());
        ExternalRequest externalRequest = externalRequests.iterator().next();
        assertEquals(1, externalRequest.getCount());
        assertEquals(host, externalRequest.getHostname());
        assertEquals("AsyncHttpClient", externalRequest.getLibrary());
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    // Fails with "DefaultAsyncHttpClient cannot be converted to AutoCloseable".
    @Trace(dispatcher = true)
    private static int makeAsyncRequest(String url) {
        DefaultAsyncHttpClient client = new DefaultAsyncHttpClient();
        try {
            BoundRequestBuilder builder = client.prepareGet(url);
            Future<Response> future = builder.execute();
            Response response = future.get();
            return response.getStatusCode();
        } catch (Exception e) {
            return -1;
        } finally {
            client.close();
        }
    }
}
