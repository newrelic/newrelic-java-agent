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
    public void testNestedValuePath() {
        assertEquals("nestedValuePath", App.nestedValuePath());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/valuePath/innerPath (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.NestedValuePath/nestedValuePath").getCallCount());
    }

    @Test
    public void testNestedPathAnnotation() {
        assertEquals("nestedPathAnnotation", App.nestedPathAnnotation());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/nestedPath/innerPath (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.NestedPathAnnotationTest/nestedPath").getCallCount());
    }

    @Test
    public void testPathAndValue() {
        assertEquals("pathAndValue", App.pathAndValue());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/path/value (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.PathAndValueTest/pathAndValue").getCallCount());
    }

    @Test
    public void testConcreteController() {
        assertEquals("concreteController", App.concreteController());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/concrete/controller/concrete (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.App/concreteController").getCallCount());
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.ConcreteControllerTest/concreteController").getCallCount());
    }

    @Test
    public void testAbstractControllerPath() {
        assertEquals("abstractControllerPath", App.abstractControllerPath());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/abstract (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.App/abstractControllerPath").getCallCount());
    }

    @Test
    public void testAbstractControllerNoPath() {
        assertEquals("abstractControllerNoPath", App.abstractControllerNoPath());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/AbstractControllerTest (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.App/abstractControllerNoPath").getCallCount());
    }

    @Test
    public void testKotlinDefaultParameter() {
        assertEquals("kotlinDefaultParameter", App.kotlinDefaultParameter());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/kotlin/read (GET)";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.KotlinSpringClass/read").getCallCount());
    }

    @Test
    public void testGet() {
        assertEquals("getmapping", App.get());
        checkVerb("Get", "getMapping", "GET");
    }

    @Test
    public void testPut() {
        assertEquals("putmapping", App.put());
        checkVerb("Put", "putMapping", "PUT");
    }

    @Test
    public void testPost() {
        assertEquals("postmapping", App.post());
        checkVerb("Post", "postMapping", "POST");
    }

    @Test
    public void testPatch() {
        assertEquals("patchmapping", App.patch());
        checkVerb("Patch", "patchMapping", "PATCH");
    }

    @Test
    public void testDelete() {
        assertEquals("deletemapping", App.delete());
        checkVerb("Delete", "deleteMapping", "DELETE");
    }

    public void checkVerb(String path, String method, String verb) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/SpringController/verb/" + path + " (" + verb + ")";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/com.nr.agent.instrumentation.VerbTests/" + method).getCallCount());
    }
}
