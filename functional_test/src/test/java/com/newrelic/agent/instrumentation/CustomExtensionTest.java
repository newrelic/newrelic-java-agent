/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.instrument.UnmodifiableClassException;
import java.util.Map;

public class CustomExtensionTest {

    public static class ClassToRetransformObject {
        private final int val;

        public ClassToRetransformObject(int val) {
            this.val = val;
        }

        // Instrumented by customExt.xml
        public int methodNoParams() {
            return val;
        }

        // Instrumented by customExt.xml
        public int methodWithParams(int timesX) {
            return val * timesX;
        }

        // Instrumented by customExt.xml
        public int methodWithSameName() {
            return val * 3;
        }

        // Instrumented by customExt.xml
        public int methodWithSameName(int timesX) {
            return val * timesX;
        }
    }

    @BeforeClass
    public static void beforeClass() {
        ServiceFactory.getClassTransformerService().getClassTransformer().getClassNameFilter().addIncludeClass(
                CustomExtensionTest.class.getName().replace('.', '/') + "$ClassToRetransformObject");
    }

    @Before
    public void clearMetrics() throws UnmodifiableClassException {
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "NOMATCH",
                "(I)V");
        InstrumentTestUtils.getAndClearMetricData();
    }

    @Test
    public void testNoParamsInstrumentation() throws Exception {
        String className = ClassToRetransformObject.class.getName();
        InstrumentTestUtils.createTransformerAndRetransformClass(className, "NOMATCH", "(I)V");

        String constructorMetric = MetricNames.SUPPORTABILITY_INIT + className + "/<init>(I)V";
        String methodMetric = MetricNames.SUPPORTABILITY_INIT + className + "/methodNoParams()I";
        Assert.assertEquals(1, new ClassToRetransformObject(1).methodNoParams());

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric));
    }

    @Test
    public void testWithParamsInstrumentation() throws Exception {
        String className = ClassToRetransformObject.class.getName();
        InstrumentTestUtils.createTransformerAndRetransformClass(className, "NOMATCH", "()V");

        String constructorMetric = MetricNames.SUPPORTABILITY_INIT + className + "/<init>(I)V";
        String methodMetric = MetricNames.SUPPORTABILITY_INIT + className + "/methodWithParams(I)I";
        Assert.assertEquals(1, new ClassToRetransformObject(1).methodNoParams());
        Assert.assertEquals(2, new ClassToRetransformObject(1).methodWithParams(2));

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric));
    }

    @Test
    public void testSameNameInstrumentation() throws Exception {
        String className = ClassToRetransformObject.class.getName();
        InstrumentTestUtils.createTransformerAndRetransformClass(className, "NOMATCH", "()V");

        String constructorMetric = MetricNames.SUPPORTABILITY_INIT + className + "/<init>(I)V";
        String mnNoArg = MetricNames.SUPPORTABILITY_INIT + className + "/methodWithSameName()I";
        String mnWithArg = MetricNames.SUPPORTABILITY_INIT + className + "/methodWithSameName(I)I";
        Assert.assertEquals(3, new ClassToRetransformObject(1).methodWithSameName());
        Assert.assertEquals(4, new ClassToRetransformObject(1).methodWithSameName(4));

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(mnNoArg));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(mnWithArg));
    }

    @Test
    @Trace(dispatcher = true)
    public void testRecordPrimitiveParam() throws Exception {
        PublicApi original = AgentBridge.publicApi;
        PublicApi api = Mockito.mock(PublicApi.class);
        AgentBridge.publicApi = api;
        try {
            new com.newrelic.agent.instrumentation.CustomExtensionTest.ClassToRetransformObject(0).methodWithParams(666);
            Mockito.verify(api).addCustomParameter("dollarz", 666);
        } finally {
            AgentBridge.publicApi = original;
        }
    }

}
