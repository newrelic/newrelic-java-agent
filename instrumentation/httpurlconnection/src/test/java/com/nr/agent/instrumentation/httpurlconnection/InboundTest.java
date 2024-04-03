/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the methods that receive data from the downstream server and thus create the external.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "leave-me-alone" }, configName = "distributed_tracing.yml")
public class InboundTest {
    @Rule
    public HttpServerRule server = new HttpServerRule();

    /**
     * Objective: make sure that a single call to an instance reports the metric and segment as expected.
     */
    @Test
    public void shouldSetMetricsAndSegmentWhenCalledOnce() throws Exception {
        runAndVerifyFirstCall(new InstrumentedHttpUrlConnection(server), 1);
    }

    /**
     * Objective: make sure that two calls to an instance report the metric once and
     * two segments, one of which is External/ and the other is Custom/
     */
    @Test
    public void shouldSetMetricsAndSegmentOnceWhenCalledTwice() throws Exception {
        int nCalls = 2;
        TraceSegment callGetResponseCodeSegment = runAndVerifyFirstCall(new InstrumentedHttpUrlConnection(server), nCalls);

        // The second call to getResponseCode is named Custom/../methodName because it doesn't result in additional I/O.
        TraceSegment defaultName = callGetResponseCodeSegment.getChildren().get(1);
        assertEquals(1, defaultName.getCallCount());
        assertEquals("Custom/" + InstrumentedHttpUrlConnection.class.getName() + "/getResponseCode", defaultName.getName());
    }

    private TraceSegment runAndVerifyFirstCall(InstrumentedHttpUrlConnection conn, int nCalls) throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        callGetResponseCode(conn, nCalls);

        // The name of the @Trace(dispatcher = true) method
        String transactionName = fetchTransactionName(introspector, "callGetResponseCode");

        // We only have one call to this external metric.
        assertTrue(introspector.getMetricsForTransaction(transactionName).containsKey("External/localhost/HttpURLConnection/getResponseCode"));
        assertEquals(1, introspector.getMetricsForTransaction(transactionName).get("External/localhost/HttpURLConnection/getResponseCode").getCallCount());

        // The number of segments is equal to the number of calls to getResponseCode.
        TransactionTrace trace = introspector.getTransactionTracesForTransaction(transactionName).iterator().next();
        TraceSegment callGetResponseCodeSegment = trace.getInitialTraceSegment();
        assertEquals(nCalls, callGetResponseCodeSegment.getChildren().size());

        // Only the first call is named External/.../getResponseCode
        TraceSegment renamedSegment = callGetResponseCodeSegment.getChildren().get(0);
        assertEquals(1, renamedSegment.getCallCount());
        assertEquals("External/localhost/HttpURLConnection/getResponseCode", renamedSegment.getName());
        return callGetResponseCodeSegment;
    }

    /**
     * Objective: Calling getResponseCode, then getInputStream, only records the external metric once, and
     * only has one External/ segment. The second segment is Custom/.
     */
    @Test
    public void shouldBumpMetricOnceIfAlreadyHaveResponseCode() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        callGetResponseCodeThenGetInputStream();

        // Only one external call.
        String transactionName = fetchTransactionName(introspector, "callGetResponseCodeThenGetInputStream");
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        long externalMetricsCount = metricsForTransaction.keySet().stream().filter(metricName -> metricName.startsWith("External")).count();
        assertEquals(1, externalMetricsCount);
        assertTrue(metricsForTransaction.containsKey("External/localhost/HttpURLConnection/getResponseCode"));
        assertEquals(1, metricsForTransaction.get("External/localhost/HttpURLConnection/getResponseCode").getCallCount());

        // Two child segments within the transaction.
        TransactionTrace trace = introspector.getTransactionTracesForTransaction(transactionName).iterator().next();
        TraceSegment traceSegment = trace.getInitialTraceSegment();
        assertEquals(2, traceSegment.getChildren().size());

        // The first segment is external.
        TraceSegment renamedSegment = traceSegment.getChildren().get(0);
        assertEquals(1, renamedSegment.getCallCount());
        assertEquals("External/localhost/HttpURLConnection/getResponseCode", renamedSegment.getName());

        // The second is a normal method call.
        TraceSegment defaultName = traceSegment.getChildren().get(1);
        assertEquals(1, defaultName.getCallCount());
        assertEquals("Custom/" + InstrumentedHttpUrlConnection.class.getName() + "/getInputStream", defaultName.getName());
    }

    /**
     * Objective: Calling getInputStream, then getResponseCode, only records the external metric once, and
     * only has one External/ segment. The second segment is Custom/.
     */
    @Test
    public void shouldBumpMetricOnceIfAlreadyHaveInputStream() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        callGetInputStreamThenResponseCode();

        // Only one external call.
        String transactionName = fetchTransactionName(introspector, "callGetInputStreamThenResponseCode");
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        long externalMetricsCount = metricsForTransaction.keySet().stream().filter(metricName -> metricName.startsWith("External")).count();
        assertEquals(1, externalMetricsCount);
        assertTrue(metricsForTransaction.containsKey("External/localhost/HttpURLConnection/getInputStream"));
        assertEquals(1, metricsForTransaction.get("External/localhost/HttpURLConnection/getInputStream").getCallCount());

        // Two child segments within the transaction.
        TransactionTrace trace = introspector.getTransactionTracesForTransaction(transactionName).iterator().next();
        TraceSegment traceSegment = trace.getInitialTraceSegment();
        assertEquals(2, traceSegment.getChildren().size());

        // The first segment is external.
        TraceSegment renamedSegment = traceSegment.getChildren().get(0);
        assertEquals(1, renamedSegment.getCallCount());
        assertEquals("External/localhost/HttpURLConnection/getInputStream", renamedSegment.getName());

        // The second is a normal method call.
        TraceSegment defaultName = traceSegment.getChildren().get(1);
        assertEquals(1, defaultName.getCallCount());
        assertEquals("Custom/" + InstrumentedHttpUrlConnection.class.getName() + "/getResponseCode", defaultName.getName());
    }

    @Test
    public void connectResponseMessageTest() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        callConnectThenGetMessage();

        // Only one external call.
        String transactionName = fetchTransactionName(introspector, "callConnectThenGetMessage");
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);
        long externalMetricsCount = metricsForTransaction.keySet().stream().filter(metricName -> metricName.startsWith("External")).count();
        assertEquals(1, externalMetricsCount);
        assertTrue(metricsForTransaction.containsKey("External/localhost/HttpURLConnection/getResponseMessage"));
        assertEquals(1, metricsForTransaction.get("External/localhost/HttpURLConnection/getResponseMessage").getCallCount());

        // Two child segments within the transaction.
        TransactionTrace trace = introspector.getTransactionTracesForTransaction(transactionName).iterator().next();
        TraceSegment traceSegment = trace.getInitialTraceSegment();
        assertEquals(2, traceSegment.getChildren().size());

        // The first s.
        TraceSegment defaultName = traceSegment.getChildren().get(0);
        assertEquals(1, defaultName.getCallCount());
        assertEquals("Custom/" + InstrumentedHttpUrlConnection.class.getName() + "/connect", defaultName.getName());

        // The second is a normal method call.
        TraceSegment renamedSegment = traceSegment.getChildren().get(1);
        assertEquals(1, renamedSegment.getCallCount());
        assertEquals("External/localhost/HttpURLConnection/getResponseMessage", renamedSegment.getName());
    }

    private String fetchTransactionName(Introspector introspector, String expectedMethod) {
        assertEquals(1, introspector.getFinishedTransactionCount(500));
        String transactionName = introspector.getTransactionNames().iterator().next();
        boolean foundExpectedEvent = false;
        for (TransactionEvent event : introspector.getTransactionEvents(transactionName)) {
            foundExpectedEvent = foundExpectedEvent || event.getName().endsWith("/" + expectedMethod);
        }
        assertTrue("Did not find an event ending in " + expectedMethod, foundExpectedEvent);
        return transactionName;
    }

    @Trace(dispatcher = true) // NOTE: Method name is used as a string in the metric!
    private void callGetResponseCode(InstrumentedHttpUrlConnection target, int nCalls) throws Exception {
        assertTrue(nCalls > 0);

        for (int i = 0; i < nCalls; i++) {
            target.getResponseCode();
        }
    }

    @Trace(dispatcher = true)
    private void callGetResponseCodeThenGetInputStream() throws Exception {
        InstrumentedHttpUrlConnection connection = new InstrumentedHttpUrlConnection(server);
        connection.getResponseCode();
        connection.getInputStream(true);
    }

    @Trace(dispatcher = true)
    private void callGetInputStreamThenResponseCode() throws Exception {
        InstrumentedHttpUrlConnection connection = new InstrumentedHttpUrlConnection(server);
        connection.getInputStream(false);
        connection.getResponseCode();
    }

    @Trace(dispatcher = true)
    private void callConnectThenGetMessage() throws Exception {
        InstrumentedHttpUrlConnection connection = new InstrumentedHttpUrlConnection(server);
        connection.connect(false);
        connection.getResponseMessage();
    }
}
