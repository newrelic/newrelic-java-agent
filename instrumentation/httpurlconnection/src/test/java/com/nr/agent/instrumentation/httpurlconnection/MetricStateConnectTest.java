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
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "leave-me-alone" }, configName = "distributed_tracing.yml")
public class MetricStateConnectTest {

    @Rule
    public HttpServerRule server = new HttpServerRule();

    private URL getURL() throws Exception {
        return server.getEndPoint().toURL();
    }

    @Test
    public void shouldSetMetricOnFirstConnect() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = runTransactionAndIntrospect(introspector, false);

        TransactionTrace trace = introspector.getTransactionTracesForTransaction(transactionName).iterator().next();

        assertEquals("External/localhost/HttpURLConnection/connect", trace.getInitialTraceSegment().getName());
        assertTrue(introspector.getMetricsForTransaction(transactionName).containsKey("External/localhost/HttpURLConnection"));

        String defaultMetricName = transactionName.replace("OtherTransaction/Custom", "Java");
        assertNotEquals(defaultMetricName, trace.getInitialTraceSegment().getName());
        assertFalse(introspector.getMetricsForTransaction(transactionName).containsKey(defaultMetricName));
    }

    @Test
    public void shouldNotSetMetricOnSecondConnect() throws Exception {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = runTransactionAndIntrospect(introspector, true);

        TransactionTrace trace = introspector.getTransactionTracesForTransaction(transactionName).iterator().next();

        assertNotEquals("External/localhost/HttpURLConnection/connect", trace.getInitialTraceSegment().getName());
        assertFalse(introspector.getMetricsForTransaction(transactionName).containsKey("External/localhost/HttpURLConnection"));

        String defaultMetricName = transactionName.replace("OtherTransaction/Custom", "Java");
        assertEquals(defaultMetricName, trace.getInitialTraceSegment().getName());
        assertTrue(introspector.getMetricsForTransaction(transactionName).containsKey(defaultMetricName));
    }


    private String runTransactionAndIntrospect(Introspector introspector, boolean pretendToBeConnected) throws Exception {
        MetricState target = new MetricState();
        callConnect(target, pretendToBeConnected);

        assertEquals(1, introspector.getFinishedTransactionCount(500));

        String transactionName = introspector.getTransactionNames().iterator().next();
        boolean foundExpectedEvent = false;
        for (TransactionEvent event : introspector.getTransactionEvents(transactionName)) {
            foundExpectedEvent = foundExpectedEvent || event.getName().endsWith("/callConnect");
        }
        assertTrue("Did not find an event ending in callConnect", foundExpectedEvent);
        return transactionName;
    }

    @Trace(dispatcher = true) // NOTE: Method name is used as a string in the metric!
    public void callConnect(MetricState target, boolean pretendToBeConnected) throws Exception {
        URLConnection connection = getURL().openConnection();
        Assert.assertTrue(connection instanceof HttpURLConnection);
        HttpURLConnection conn = (HttpURLConnection) connection;

        target.nonNetworkPreamble(pretendToBeConnected, conn, "connect");
    }
}
