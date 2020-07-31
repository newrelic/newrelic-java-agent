/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ClassRetransformStaticTest {

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
     * Basic test which verifies that we can retransform a static method.
     * 
     * @throws Exception
     */
    @Test
    public void testClassRetransformStaticMethod() throws Exception {
        String transactionMetric = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getRandomInt";
        String methodMetric = "Java/" + ClassToRetransformObject.class.getName() + "/getRandomInt";

        // load class
        ClassToRetransformObject.getRandomInt();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getRandomInt",
                "()I");

        // call method on class that was transformed
        ClassToRetransformObject.getRandomInt();

        // verifiy
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }

}
