/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AllMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InstrumentationMetricsTest {

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

    public static class ClassToRetransform2Object {
        private final int val;

        public ClassToRetransform2Object(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

        public int getVal2() {
            return val * 2;
        }
    }

    public static class ClassToRetransform3Object {
        private final int val;

        public ClassToRetransform3Object(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

        public int getVal2() {
            return val * 2;
        }
    }

    @Test
    public void testTransformationMetrics() throws Exception {
        ServiceFactory.getClassTransformerService().getClassTransformer().getClassNameFilter().addIncludeClass(
                getClass().getName().replace('.', '/') + "$ClassToRetransform");

        // Clear out existing metrics
        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // This will load the class also:
        String className = ClassToRetransformObject.class.getName();

        String constructorMetric = MetricNames.SUPPORTABILITY_INIT + className + "/<init>(I)V";
        String methodMetric1 = MetricNames.SUPPORTABILITY_INIT + className + "/getVal()I";
        String methodMetric2 = MetricNames.SUPPORTABILITY_INIT + className + "/getVal2()I";
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(className, "<init>", "(I)V");
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(className, "NONEXISTENT", "(I)V");
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(className, "getVal", "()I");
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        InstrumentTestUtils.createTransformerAndRetransformClass(className, "getVal2", "()I");
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        Assert.assertEquals(2, new ClassToRetransformObject(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertEquals(Integer.valueOf(1), metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric2));
    }

    @Test
    public void testMultiplePointcutsTransformationMetrics() throws Exception {
        ServiceFactory.getClassTransformerService().getClassTransformer().getClassNameFilter().addIncludeClass(
                getClass().getName().replace('.', '/') + "$ClassToRetransform2");

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // This will load the class also:
        String className = ClassToRetransform2Object.class.getName();

        String constructorMetric = MetricNames.SUPPORTABILITY_INIT + className + "/<init>(I)V";
        String methodMetric1 = MetricNames.SUPPORTABILITY_INIT + className + "/getVal()I";
        String methodMetric2 = MetricNames.SUPPORTABILITY_INIT + className + "/getVal2()I";
        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());
        Assert.assertEquals(2, new ClassToRetransform2Object(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertNull(metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        ExtensionClassAndMethodMatcher pc1 = new ExtensionClassAndMethodMatcher(null, null, "Custom",
                new ExactClassMatcher(className), new ExactMethodMatcher("getVal", "()I"), false, false, false, false, null);
        ExtensionClassAndMethodMatcher pc2 = new ExtensionClassAndMethodMatcher(null, null, "Custom",
                new ExactClassMatcher(className), new ExactMethodMatcher("getVal2", "()I"), false, false, false, false, null);

        List<ExtensionClassAndMethodMatcher> list = new LinkedList<>();
        list.add(pc1);
        list.add(pc2);
        ServiceFactory.getClassTransformerService().getLocalRetransformer().appendClassMethodMatchers(list);

        InstrumentTestUtils.retransformClass(className);

        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());
        Assert.assertEquals(2, new ClassToRetransform2Object(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric2));
    }

    @Test
    public void testMultipleMethodTransformationMetrics() throws Exception {
        ServiceFactory.getClassTransformerService().getClassTransformer().getClassNameFilter().addIncludeClass(
                getClass().getName().replace('.', '/') + "$ClassToRetransform3");

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();

        // This will load the class also:
        String className = ClassToRetransform3Object.class.getName();

        String constructorMetric = MetricNames.SUPPORTABILITY_INIT + className + "/<init>(I)V";
        String methodMetric1 = MetricNames.SUPPORTABILITY_INIT + className + "/getVal()I";
        String methodMetric2 = MetricNames.SUPPORTABILITY_INIT + className + "/getVal2()I";
        Assert.assertEquals(1, new ClassToRetransform3Object(1).getVal());
        Assert.assertEquals(2, new ClassToRetransform3Object(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertNull(metricData.get(methodMetric1));
        Assert.assertNull(metricData.get(methodMetric2));

        ExtensionClassAndMethodMatcher pc1 = new ExtensionClassAndMethodMatcher(null, null, "Custom",
                new ExactClassMatcher(className), new AllMethodsMatcher(), false, false, false, false, null);

        List<ExtensionClassAndMethodMatcher> list = new LinkedList<>();
        list.add(pc1);
        ServiceFactory.getClassTransformerService().getLocalRetransformer().appendClassMethodMatchers(list);
        InstrumentTestUtils.retransformClass(className);

        Assert.assertEquals(1, new ClassToRetransform3Object(1).getVal());
        Assert.assertEquals(2, new ClassToRetransform3Object(1).getVal2());

        metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric1));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric2));
    }
}
