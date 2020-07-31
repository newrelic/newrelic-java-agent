/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ClassRetransformMultipleTimesTest {

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
        @InstrumentedMethod(dispatcher = false, instrumentationNames = "dude",
                instrumentationTypes = InstrumentationType.Unknown)
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
     * Tests retransforming the same method which was transformed on the initial load.
     * 
     * @throws Exception
     */
    @Test
    public void testMethodListedTwice() throws Exception {
        String methodMetric = "Java/" + ClassToRetransformObject.class.getName() + "/getTripleValue";
        // call the method
        String transactionMetric = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName()
                + "/getTripleValue";
        ClassToRetransformObject theClass = new ClassToRetransformObject(3);
        theClass.getTripleValue();

        // verify that it has been initially transformed
        // it should have been transformed from the initial xml_files/customExt.xml
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // warning: this is going to do a harvest, meaning you will loose the metric
        InstrumentTestUtils.verifyCountMetric(expected);

        InstrumentedMethod annotation = ClassToRetransformObject.class.getMethod("getTripleValue").getAnnotation(
                InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals("customExtensions", annotation.instrumentationNames()[0]);
        Assert.assertEquals(InstrumentationType.LocalCustomXml, annotation.instrumentationTypes()[0]);

        // retransform same method
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getTripleValue", "()I");

        // call the method
        theClass = new ClassToRetransformObject(3);
        theClass.getTripleValue();

        // verify that the method was only called once
        expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        InstrumentTestUtils.verifyCountMetric(expected);

        annotation = ClassToRetransformObject.class.getMethod("getTripleValue").getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        // these should always be the same length
        Assert.assertEquals(annotation.instrumentationTypes().length, annotation.instrumentationNames().length);
        for (int i = 0; i < annotation.instrumentationTypes().length; i++) {
            if (InstrumentationType.LocalCustomXml == annotation.instrumentationTypes()[i]) {
                Assert.assertEquals("customExtensions", annotation.instrumentationNames()[i]);
            } else if (InstrumentationType.CustomYaml == annotation.instrumentationTypes()[i]) {
                // this should really be a remotecustomxml but it is set up to be custom yaml
                Assert.assertEquals("mytest", annotation.instrumentationNames()[i]);
            } else {
                Assert.fail("The instrumentation type should be local custom xml and custom yaml");
            }
        }

    }

    /**
     * Tests retransforming the same method twice.
     * 
     * @throws Exception
     */
    @Test
    public void testMethodListedTwiceRetransformation() throws Exception {
        String methodMetric = "Java/" + ClassToRetransformObject.class.getName() + "/getValue";
        // call the method
        String transactionMetric = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getValue";
        ClassToRetransformObject theClass = new ClassToRetransformObject(3);
        theClass.getValue();

        // we should not see any metric since the method is not instrumented
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);
        InstrumentTestUtils.verifyMetricNotPresent(methodMetric);

        // retransform method
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getValue",
                "()I");

        // retransform same method
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getValue",
                "()I");

        // call the method
        theClass = new ClassToRetransformObject(3);
        theClass.getValue();

        // verify that the method was only called once
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        InstrumentTestUtils.verifyCountMetric(expected);

    }
}
