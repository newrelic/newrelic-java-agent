/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "leave-me-alone" }, configName = "distributed_tracing.yml")
public class MetricStateResponseCodeTest {
    /**
     * Objective: make sure that a single call to an instance reports the metric and segment as expected.
     */
    @Test
    public void shouldSetMetricsAndSegmentWhenCalledOnce() throws Exception {
        MetricState target = new MetricState();
        runAndVerifyFirstCall(target, 1);
    }

    /**
     * Objective: make sure that two calls to an instance report the metric once and
     * two segments, one of which is External/ and the other is Custom/
     */
    @Test
    public void shouldSetMetricsAndSegmentOnceWhenCalledTwice() throws Exception {
        MetricState target = new MetricState();
        int nCalls = 2;
        TraceSegment callGetResponseCodeSegment = runAndVerifyFirstCall(target, nCalls);

        // The second call to getResponseCode is named Custom/../methodName because it doesn't result in additional I/O.
        TraceSegment defaultName = callGetResponseCodeSegment.getChildren().get(1);
        assertEquals(1, defaultName.getCallCount());
        assertEquals("Custom/" + this.getClass().getName() + "/simulatedInstrumentedGetResponseCodeMethod", defaultName.getName());
    }

    private TraceSegment runAndVerifyFirstCall(MetricState target, int nCalls) throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        callGetResponseCode(target, nCalls);

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
        MetricState target = new MetricState();
        callGetResponseCodeThenGetInputStream(target);

        // Only one external call.
        String transactionName = fetchTransactionName(introspector, "callGetResponseCodeThenGetInputStream");
        assertTrue(introspector.getMetricsForTransaction(transactionName).containsKey("External/localhost/HttpURLConnection/getResponseCode"));
        assertEquals(1, introspector.getMetricsForTransaction(transactionName).get("External/localhost/HttpURLConnection/getResponseCode").getCallCount());

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
        assertEquals("Custom/" + this.getClass().getName() + "/simulatedInstrumentedGetInputStreamMethod", defaultName.getName());
    }

    /**
     * Objective: Calling getInputStream, then getResponseCode, only records the external metric once, and
     * only has one External/ segment. The second segment is Custom/.
     */
    @Test
    public void shouldBumpMetricOnceIfAlreadyHaveInputStream() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        MetricState target = new MetricState();
        callGetInputStreamThenResponseCode(target);

        // Only one external call.
        String transactionName = fetchTransactionName(introspector, "callGetInputStreamThenResponseCode");
        assertTrue(introspector.getMetricsForTransaction(transactionName).containsKey("External/localhost/HttpURLConnection/getInputStream"));
        assertEquals(1, introspector.getMetricsForTransaction(transactionName).get("External/localhost/HttpURLConnection/getInputStream").getCallCount());

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
        assertEquals("Custom/" + this.getClass().getName() + "/simulatedInstrumentedGetResponseCodeMethod", defaultName.getName());
    }

    private String fetchTransactionName(Introspector introspector, String expectedMethod) {
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String transactionName = introspector.getTransactionNames().iterator().next();
        boolean foundExpectedEvent = false;
        for (TransactionEvent event : introspector.getTransactionEvents(transactionName)) {
            foundExpectedEvent = foundExpectedEvent || event.getName().endsWith("/" + expectedMethod);
        }
        assertTrue("Did not find an event ending in callGetResponseCode", foundExpectedEvent);
        return transactionName;
    }

    @Rule
    public HttpServerRule server = new HttpServerRule();

    private URL getURL() throws Exception {
        return server.getEndPoint().toURL();
    }

    @Trace(dispatcher = true) // NOTE: Method name is used as a string in the metric!
    public void callGetResponseCode(MetricState target, int nCalls) throws Exception {
        assertTrue(nCalls > 0);

        URLConnection connection = getURL().openConnection();
        assertTrue(connection instanceof HttpURLConnection);
        HttpURLConnection conn = (HttpURLConnection) connection;

        for (int i = 0; i < nCalls; i++) {
            simulatedInstrumentedGetResponseCodeMethod(conn, target);
        }
    }

    @Trace(dispatcher = true)
    public void callGetResponseCodeThenGetInputStream(MetricState target) throws Exception {
        URLConnection connection = getURL().openConnection();
        assertTrue(connection instanceof HttpURLConnection);
        HttpURLConnection conn = (HttpURLConnection) connection;

        simulatedInstrumentedGetResponseCodeMethod(conn, target);
        simulatedInstrumentedGetInputStreamMethod(true, conn, target);
    }

    @Trace(dispatcher = true)
    public void callGetInputStreamThenResponseCode(MetricState target) throws Exception {
        URLConnection connection = getURL().openConnection();
        assertTrue(connection instanceof HttpURLConnection);
        HttpURLConnection conn = (HttpURLConnection) connection;

        simulatedInstrumentedGetInputStreamMethod(false, conn, target);
        simulatedInstrumentedGetResponseCodeMethod(conn, target);
    }

    /**
     * The purpose of this method is to simulate the woven method from the instrumentation
     * code. Since we can't weave JRE classes in these tests, we can't use the "real" code.
     * This is the best approximation.
     */
    @Trace(leaf = true)
    private void simulatedInstrumentedGetResponseCodeMethod(HttpURLConnection conn, MetricState target) {
        target.getResponseCodePreamble(conn, AgentBridge.getAgent().getTracedMethod());
        target.getInboundPostamble(conn, 0, null, "getResponseCode", AgentBridge.getAgent().getTracedMethod());
    }

    @Trace(leaf = true)
    private void simulatedInstrumentedGetInputStreamMethod(boolean isConnected, HttpURLConnection conn, MetricState target) {
        target.getInputStreamPreamble(isConnected, conn, AgentBridge.getAgent().getTracedMethod());
        target.getInboundPostamble(conn, 0, null, "getInputStream", AgentBridge.getAgent().getTracedMethod());
    }
}
