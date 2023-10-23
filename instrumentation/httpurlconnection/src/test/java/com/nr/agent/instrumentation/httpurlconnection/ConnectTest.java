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
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "leave-me-alone" }, configName = "distributed_tracing.yml")
public class ConnectTest {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    @Test
    public void shouldSetHeadersOnFirstConnect() throws Exception {
        InstrumentedHttpUrlConnection fakeConnection = new InstrumentedHttpUrlConnection(server);
        runTransactionAndIntrospect(fakeConnection, false);

        // assert that the headers were set
        HttpURLConnection realConnection = fakeConnection.getRealConnection();
        assertNotNull(realConnection.getRequestProperty("tracestate"));
        assertNotNull(realConnection.getRequestProperty("traceparent"));
        assertNotNull(realConnection.getRequestProperty("newrelic"));
    }

    @Test
    public void shouldNotSetHeadersOnSecondConnect() throws Exception {
        InstrumentedHttpUrlConnection fakeConnection = new InstrumentedHttpUrlConnection(server);
        runTransactionAndIntrospect(fakeConnection, true);

        // assert that the headers were not set
        HttpURLConnection realConnection = fakeConnection.getRealConnection();
        assertNull(realConnection.getRequestProperty("tracestate"));
        assertNull(realConnection.getRequestProperty("traceparent"));
        assertNull(realConnection.getRequestProperty("newrelic"));
    }


    private void runTransactionAndIntrospect(InstrumentedHttpUrlConnection connection, boolean pretendToBeConnected) throws Exception {
        callConnect(connection, pretendToBeConnected);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String transactionName = introspector.getTransactionNames().iterator().next();
        boolean foundExpectedEvent = false;
        for (TransactionEvent event : introspector.getTransactionEvents(transactionName)) {
            foundExpectedEvent = foundExpectedEvent || event.getName().endsWith("/callConnect");
        }
        assertTrue("Did not find an event ending in callConnect", foundExpectedEvent);

        TransactionTrace trace = introspector.getTransactionTracesForTransaction(transactionName).iterator().next();
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);

        String defaultMetricName = transactionName.replace("OtherTransaction/Custom", "Java");
        assertEquals(defaultMetricName, trace.getInitialTraceSegment().getName());
        assertTrue(metricsForTransaction.containsKey(defaultMetricName));

        // calling connect does not create external
        long externalCount = metricsForTransaction.keySet().stream().filter(metricName -> metricName.startsWith("External")).count();
        assertEquals(0, externalCount);

        assertTrue(metricsForTransaction.containsKey("Custom/" + InstrumentedHttpUrlConnection.class.getName() + "/connect"));
    }


    @Trace(dispatcher = true) // NOTE: Method name is used as a string in the metric!
    public void callConnect(InstrumentedHttpUrlConnection connection, boolean pretendToBeConnected) throws Exception {
        connection.connect(pretendToBeConnected);
    }
}
