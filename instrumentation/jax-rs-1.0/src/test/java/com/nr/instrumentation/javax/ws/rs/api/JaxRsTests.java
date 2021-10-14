/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.instrumentation.javax.ws.rs.api" })
public class JaxRsTests {

    @Test
    public void testTransactionNamePatch() {
        assertEquals("Patched it!", App.callClassResourcePatchInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/class (PATCH)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.ClassResource/patchIt").getCallCount());
    }

    @Test
    public void testTransactionNamePut() {
        assertEquals("Put it!", App.callClassResourcePutInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/class (PUT)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.ClassResource/putIt").getCallCount());
    }

    @Test
    public void testTransactionNamePost() {
        assertEquals("Posted it!", App.callClassResourcePostInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/class (POST)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.ClassResource/postIt").getCallCount());
    }

    @Test
    public void testTransactionNamePath() {
        assertEquals("Got it!", App.callClassResourceInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/class (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.ClassResource/getIt").getCallCount());
    }

    @Test
    public void testTransactionNameInterface() {
        assertEquals("Got it from the interface!", App.callInterfaceResourceInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/interface (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.InterfaceResourceImpl/getIt").getCallCount());
    }

    @Test
    public void testUserService() {
        assertEquals("User Features!", App.callUserServiceImpl());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/v1/user/{id}/features (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.UserServiceImpl/getUserFeatures").getCallCount());
    }

    @Test
    public void testTransactionNameInterfaceWithImpl() {
        assertEquals("Returning from getPeople", App.callInterfaceWithImplResourceInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/v1/people/getPeople (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.PeopleResource/getPeople").getCallCount());
    }

    @Test
    public void testTransactionNameClassException() {
        assertEquals("Got it!", App.callExceptionClassResourceInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/class (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.ClassResource/exceptionTest").getCallCount());
    }

    @Test
    public void testTransactionNameInterfaceException() {
        assertEquals("Got it from the interface!", App.callExceptionInterfaceResourceInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/interface (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.InterfaceResourceImpl/exceptionTest").getCallCount());
    }

    @Test
    public void testTransactionNameClassLargeParameters() {
        assertEquals("Got it!", App.callLargeParametersClassResourceInTransaction());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/class (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.ClassResource/largeNumberOfParametersTest").getCallCount());
    }

    @Test
    public void testStaticEndpoint() {
        assertEquals("static endpoint was called", App.callStaticEndpoint());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/staticEndpoints/staticMethod (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.StaticEndpoint/staticMethod").getCallCount());
    }

    @Test
    public void testInnerClass() {
        assertEquals("inner class endpoint was called", App.callInnerClass());
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/RestWebService/inner/staticMethod (GET)";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(
                expectedTransactionName);
        assertEquals(1, metricsForTransaction.get(
                "Java/com.nr.instrumentation.javax.ws.rs.api.StaticEndpoint$InnerClass/innerStatic").getCallCount());
    }
}
