/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class ClassRetransformTest {

    static class ClassToRetransformObject {

        private int inputValue;

        public ClassToRetransformObject(int value) {
            inputValue = value;
        }

        public int getValue() {
            return inputValue;
        }

        public int getDoubleValue() {
            return inputValue * 2;
        }

        /**
         * This method is in a custom instrumentation file and so it should be instrumented on startup.
         *
         * @return Three times the input value.
         */
        public int getTripleValue() {
            return inputValue * 3;
        }

        public int updateAndGetValue() {
            inputValue += 1;
            return inputValue;
        }

        public static int getRandomInt() {
            return new Random().nextInt();
        }
    }

    /**
     * Instruments four methods from the same class.
     */
    @Test
    public void testClassRetransformInstanceMethodsSameClass() throws UnmodifiableClassException {
        // metric names
        String metricNameGetValue = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getValue";
        String metricMethodNameGetValue = "Java/" + ClassToRetransformObject.class.getName() + "/getValue";
        String metricNameGetDoubleValue = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName()
                + "/getDoubleValue";
        String metricMethodNameGetDoubleValue = "Java/" + ClassToRetransformObject.class.getName() + "/getDoubleValue";
        String metricNameUpdateValue = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName()
                + "/updateAndGetValue";
        String metricMethodNameUpdateValue = "Java/" + ClassToRetransformObject.class.getName() + "/updateAndGetValue";
        String transStaticMetricName = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName()
                + "/getRandomInt";
        String methodStaticMetricName = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName()
                + "/getRandomInt";
        List<String> metrics = Arrays.asList(metricNameGetValue, metricNameGetDoubleValue, metricNameUpdateValue,
                transStaticMetricName, metricMethodNameGetValue, metricMethodNameGetDoubleValue,
                metricMethodNameUpdateValue, methodStaticMetricName);

        // call all of the methods and verify no metric is created (nothing has been transformed)
        ClassToRetransformObject theClass = new ClassToRetransformObject(5);
        theClass.getValue();
        theClass.getDoubleValue();
        theClass.updateAndGetValue();
        ClassToRetransformObject.getRandomInt();
        InstrumentTestUtils.verifyMetricNotPresent(metrics);

        // retransform method
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getValue",
                "()I");

        // call method on class and verify retransformation
        theClass.getValue();
        Map<String, Integer> expected = new HashMap<>();
        expected.put(metricNameGetValue, 1);
        // warning: this is going to do a harvest, meaning you will loose the metric
        InstrumentTestUtils.verifyCountMetric(expected);

        // retransform a second method in the same class
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getDoubleValue", "()I");

        // call both methods
        theClass = new ClassToRetransformObject(6);
        theClass.getDoubleValue();
        theClass.getValue();
        theClass.getValue();

        // verify retransformations
        expected = new HashMap<>();
        // this is two instead of three because verifyCountMetric does a harvest
        expected.put(metricNameGetValue, 2);
        expected.put(metricNameGetDoubleValue, 1);
        expected.put(metricMethodNameGetValue, 2);
        expected.put(metricMethodNameGetDoubleValue, 1);
        InstrumentTestUtils.verifyCountMetric(expected);

        // retransform a third method in the same class
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "updateAndGetValue", "()I");

        // call all 3 methods
        theClass = new ClassToRetransformObject(6);
        theClass.getDoubleValue();
        theClass.getDoubleValue();
        theClass.getDoubleValue();
        theClass.updateAndGetValue();
        theClass.updateAndGetValue();
        theClass.getValue();

        // verify retransformations
        expected = new HashMap<>();
        expected.put(metricNameGetValue, 1);
        expected.put(metricNameGetDoubleValue, 3);
        expected.put(metricNameUpdateValue, 2);
        expected.put(metricMethodNameGetValue, 1);
        expected.put(metricMethodNameGetDoubleValue, 3);
        expected.put(metricMethodNameUpdateValue, 2);
        InstrumentTestUtils.verifyCountMetric(expected);

        // retransform a fourth static method on the class

        // make retransformer
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getRandomInt", "()I");

        // call the static method
        ClassToRetransformObject.getRandomInt();

        // verify
        expected = new HashMap<>();
        expected.put(transStaticMetricName, 1);
        expected.put(methodStaticMetricName, 1);
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    /**
     * Instruments a core JDK method. Note that java.util.TreeMap was added in the newrelic.yml to specifically be
     * allowed to be instrumented. Normally you are not allowed to instrument core JDK methods.
     */
    @Test
    public void testRetransformBootstrapMethods() throws Exception {
        String transMetricName = "OtherTransaction/Custom/java.util.TreeMap/containsKey";
        String methodMetricName = "Java/java.util.TreeMap/containsKey";

        // load class
        Map sample = new TreeMap();
        sample.containsKey("hi");
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transMetricName, methodMetricName));

        // make retransformer
        InstrumentTestUtils.createTransformerAndRetransformClass("java.util.TreeMap", "containsKey",
                "(Ljava/lang/Object;)Z");

        // call method on class that was transformered
        sample = new TreeMap();
        sample.containsKey("hi");
        sample.containsKey("hello");

        Map<String, Integer> expected = new HashMap<>();
        expected.put(transMetricName, 2);
        expected.put(methodMetricName, 2);
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    @Test
    public void testAsmClass() throws Exception {
        String className = ClassWriter.class.getName();
        String transMetricNewClass = InstrumentTestUtils.TRANS_PREFIX + className + "/newClass";
        String methodMetricNewClass = InstrumentTestUtils.METHOD_PREFIX + className + "/newClass";
        String transMetricVisitAnnotation = InstrumentTestUtils.TRANS_PREFIX + className + "/visitAnnotation";
        String methodMetricVisitAnnotation = InstrumentTestUtils.METHOD_PREFIX + className + "/visitAnnotation";

        callAsmClass();
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transMetricNewClass, methodMetricNewClass,
                transMetricVisitAnnotation, methodMetricVisitAnnotation));

        InstrumentTestUtils.createTransformerAndRetransformClass(className, "newClass", "(Ljava/lang/String;)I");

        callAsmClass();
        InstrumentTestUtils.verifySingleMetrics(transMetricNewClass, methodMetricNewClass);
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transMetricVisitAnnotation,
                methodMetricVisitAnnotation));

        InstrumentTestUtils.createTransformerAndRetransformClass(className, "visitAnnotation",
                "(Ljava/lang/String;Z)Lorg/objectweb/asm/AnnotationVisitor;");

        InstrumentTestUtils.getAndClearMetricData();

        callAsmClass();
        InstrumentTestUtils.verifySingleMetrics(transMetricNewClass, methodMetricNewClass, transMetricVisitAnnotation,
                methodMetricVisitAnnotation);

    }

    private void callAsmClass() throws SecurityException, NoSuchMethodException {
        ClassWriter cw = new ClassWriter(WeaveUtils.ASM_API_LEVEL);
        cw.newClass("portland.gorge");
        cw.visitAnnotation("AlreadyTested", false);

    }

}
