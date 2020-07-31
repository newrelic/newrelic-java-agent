/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.util.Map;

import org.junit.Assert;

import org.junit.Test;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;

public class APIInstrumentationTest {

    public static class ClassToRetransformObject {
        private final int val;

        public ClassToRetransformObject(int val) {
            this.val = val;
        }

        @Trace
        public int getVal() {
            return val;
        }

        public int getVal2() {
            return val * 2;
        }
    }

    @Test
    public void testAPIInstrumentation() throws Exception {
        ServiceFactory.getClassTransformerService().getClassTransformer().getClassNameFilter().addIncludeClass(
                getClass().getName().replace('.', '/') + "$ClassToRetransform");

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        String methodMetric1 = "Supportability/Instrumented/" + ClassToRetransformObject.class.getName() + "/getVal()I";
        String methodMetric2 = "Supportability/Instrumented/" + ClassToRetransformObject.class.getName()
                + "/getVal2()I";
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getVal",
                "()I");
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getVal2",
                "()I");
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getVal",
                "()I");
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getVal2",
                "()I");
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(2), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(2), metricData.get(methodMetric2));
    }

    public static class ClassToRetransform2Object {
        private final int val;

        public ClassToRetransform2Object(int val) {
            this.val = val;
        }

        @Trace
        public int getVal() {
            return val;
        }

        public int getVal2() {
            return val * 2;
        }
    }

    @Test
    public void testAPIInstrumentationReverse() throws Exception {
        ServiceFactory.getClassTransformerService().getClassTransformer().getClassNameFilter().addIncludeClass(
                getClass().getName().replace('.', '/') + "$ClassToRetransform2");

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        String methodMetric1 = "Supportability/Instrumented/" + ClassToRetransform2Object.class.getName() + "/getVal()I";
        String methodMetric2 = "Supportability/Instrumented/" + ClassToRetransform2Object.class.getName() + "/getVal2()I";
        Assert.assertEquals(2, new ClassToRetransform2Object(1).getVal2());
        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform2Object.class.getName(), "getVal2", "()I");
        Assert.assertEquals(2, new ClassToRetransform2Object(1).getVal2());
        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform2Object.class.getName(), "getVal", "()I");
        Assert.assertEquals(2, new ClassToRetransform2Object(1).getVal2());
        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform2Object.class.getName(), "getVal", "()I");
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform2Object.class.getName(), "getVal2", "()I");
        Assert.assertEquals(2, new ClassToRetransform2Object(1).getVal2());
        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(2), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(2), metricData.get(methodMetric2));
    }
}
