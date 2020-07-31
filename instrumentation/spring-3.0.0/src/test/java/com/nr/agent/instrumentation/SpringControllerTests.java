/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.nr.agent.instrumentation" })
public class SpringControllerTests {

    @Test
    public void testErrorPath() {
        App.error();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/errorPath (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.ErrorPath/testError").getCallCount());
    }

    @Test
    public void testPathClass() {
        assertEquals("PathClass", App.pathClass());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/pathClass/methodTestPath (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.PathClass/testPath").getCallCount());
    }

    @Test
    public void testInnerPath() {
        assertEquals("innerPath", App.innerPath());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/defaultPath/innerPath (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.TestInnerAndDefaultPath/testInnerPath").getCallCount());
    }

    @Test
    public void testMethodPath() {
        assertEquals("methodPath", App.methodPath());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/pathTest (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.TestPathAnnotationForMethod/testPathAnnotation").getCallCount());
    }

    @Test
    public void testKotlinDefaultParameter() {
        assertEquals("kotlinDefaultParameter", App.kotlinDefaultParameter());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/kotlin/read (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.KotlinSpringClass/read").getCallCount());
    }
}
