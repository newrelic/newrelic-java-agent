/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws;

import com.newrelic.agent.introspec.CatHelper;
import com.newrelic.agent.introspec.ExternalRequest;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.Java10IncompatibleTest;
import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java12IncompatibleTest;
import com.newrelic.test.marker.Java13IncompatibleTest;
import com.newrelic.test.marker.Java14IncompatibleTest;
import com.newrelic.test.marker.Java15IncompatibleTest;
import com.newrelic.test.marker.Java16IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java9IncompatibleTest;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import play.api.libs.ws.StandaloneWSResponse;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({ Java9IncompatibleTest.class, Java10IncompatibleTest.class, Java11IncompatibleTest.class, Java12IncompatibleTest.class,
        Java13IncompatibleTest.class, Java14IncompatibleTest.class, Java15IncompatibleTest.class, Java16IncompatibleTest.class, Java17IncompatibleTest.class })
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation.playws", "play" })
public class PlayAsyncHttpClientTests {
    @ClassRule
    public static HttpServerRule server = new HttpServerRule();

    @Test
    public void testCat() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger();

        makeAsyncRequest(endpoint.toURL().toString(), new ResponseRunnable() {
            @Override
            public void onResponse(StandaloneWSResponse standaloneWSResponse) {
                result.set(standaloneWSResponse.status());
                latch.countDown();
            }
        });

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(200, result.get());

        // transaction
        String txName = "OtherTransaction/Custom/com.nr.agent.instrumentation.playws.PlayAsyncHttpClientTests/makeAsyncRequest";
        assertEquals(2, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(30)));
        Collection<String> names = introspector.getTransactionNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(server.getServerTransactionName()));
        assertTrue(names.contains(txName));

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                "Java/com.nr.agent.instrumentation.playws.PlayAsyncHttpClientTests/makeAsyncRequest"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("ExternalTransaction/" + host + "/"
                + server.getCrossProcessId() + "/" + server.getServerTransactionName()));
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/all"));
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
        assertEquals(host, externalRequest.getHostname());
    }

    @Test
    public void testSuccess() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        String url = endpoint.toURL().toExternalForm();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger();
        makeAsyncRequest(url + "?no-transaction=true", standaloneWSResponse -> {
            result.set(standaloneWSResponse.status());
            latch.countDown();
        });

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(200, result.get());

        // transaction
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(30)));
        String txName = introspector.getTransactionNames().iterator().next();

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/" + host
                + "/PlayWS/get"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/PlayWS/get"));
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
            if (attributes.get("async_context").equals("segment-api")) {
                assertEquals("", segment.getMethodName());
                assertEquals("PlayWS", segment.getClassName());
                assertEquals("External/" + host + "/PlayWS/get", segment.getName());
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
        assertEquals("PlayWS", externalRequest.getLibrary());
        assertEquals("get", externalRequest.getOperation());
    }

    @Test
    public void testError() {
        String host = "www.thiswebsitedoesntexistreallyatallipromise.com";
        String url = "http://" + host + "/";

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger();
        makeAsyncRequest(url, standaloneWSResponse -> {
            result.set(standaloneWSResponse.status());
            latch.countDown();
        });
        assertEquals(0, result.get());

        // transaction
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(30)));
        String txName = introspector.getTransactionNames().iterator().next();

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/" + host + "/PlayWS/get"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/PlayWS/get"));
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
            if ("segment-api".equals(attributes.get("async_context"))) {
                assertEquals("", segment.getMethodName());
                assertEquals("PlayWS", segment.getClassName());
                assertEquals("External/" + host + "/PlayWS/get", segment.getName());
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
        assertEquals("PlayWS", externalRequest.getLibrary());
    }

    @Test
    public void testWithHeaders() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        String url = endpoint.toURL().toExternalForm();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger();
        makeAsyncRequestWithHeaders(url + "?no-transaction=true", standaloneWSResponse -> {
            result.set(standaloneWSResponse.status());
            latch.countDown();
        });

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(200, result.get());

        // transaction
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(30)));
        String txName = introspector.getTransactionNames().iterator().next();

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/" + host
                + "/PlayWS/get"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/PlayWS/get"));
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
            if (attributes.get("async_context").equals("segment-api")) {
                assertEquals("", segment.getMethodName());
                assertEquals("PlayWS", segment.getClassName());
                assertEquals("External/" + host + "/PlayWS/get", segment.getName());
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
        assertEquals("PlayWS", externalRequest.getLibrary());
        assertEquals("get", externalRequest.getOperation());
    }

    @Test
    public void testWithFilterHeaders() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        URI endpoint = server.getEndPoint();
        String host = endpoint.getHost();
        String url = endpoint.toURL().toExternalForm();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger();
        makeAsyncRequestWithFilterHeaders(url + "?no-transaction=true", standaloneWSResponse -> {
            result.set(standaloneWSResponse.status());
            latch.countDown();
        });

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(200, result.get());

        // transaction
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(30)));
        String txName = introspector.getTransactionNames().iterator().next();

        // scoped metrics
        assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/" + host
                + "/PlayWS/get"));

        // unscoped metrics
        assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/" + host + "/PlayWS/get"));
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
            if (attributes.get("async_context").equals("segment-api")) {
                assertEquals("", segment.getMethodName());
                assertEquals("PlayWS", segment.getClassName());
                assertEquals("External/" + host + "/PlayWS/get", segment.getName());
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
        assertEquals("PlayWS", externalRequest.getLibrary());
        assertEquals("get", externalRequest.getOperation());
    }

    @Trace(dispatcher = true)
    private static void makeAsyncRequest(String url, ResponseRunnable responseRunnable) {
        WSClient.makeRequest(url, responseRunnable);
    }

    @Trace(dispatcher = true)
    private static void makeAsyncRequestWithHeaders(String url, ResponseRunnable responseRunnable) {
        WSClient.makeRequestWithHeaders(url, responseRunnable);
    }

    @Trace(dispatcher = true)
    private static void makeAsyncRequestWithFilterHeaders(String url, ResponseRunnable responseRunnable) {
        WSClient.makeRequestWithFilterHeaders(url, responseRunnable);
    }
}
