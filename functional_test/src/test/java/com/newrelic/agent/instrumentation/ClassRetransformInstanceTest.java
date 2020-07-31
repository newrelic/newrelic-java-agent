/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Test;

import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This is a basic class which just tests the transformation of one class and method.
 * 
 * @since May 31, 2013
 */
public class ClassRetransformInstanceTest {

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

    @Test
    public void testClassRetransformInstanceMethod() throws UnmodifiableClassException {
        String transactionName = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getDoubleValue";
        String methodMetric = "Java/" + ClassToRetransformObject.class.getName() + "/getDoubleValue";

        // load class
        ClassToRetransformObject theClass = new ClassToRetransformObject(5);
        theClass.getDoubleValue();
        InstrumentTestUtils.verifyMetricNotPresent(transactionName);

        InstrumentTestUtils.createTransformerAndRetransformClass(
                ClassToRetransformObject.class.getName(), "getDoubleValue", "()I");

        // call method on class that was transformered
        theClass = new ClassToRetransformObject(6);
        theClass.getDoubleValue();

        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionName, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // call method and verify metrics once again
        theClass.getDoubleValue();
        InstrumentTestUtils.verifyCountMetric(expected);

    }

}
