/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Assert;

import org.junit.Test;

public class ClassRetransformPublicPrivateProtectedMethodsTest {

    static class ClassToRetransformObject {

        private int inputValue;

        public ClassToRetransformObject(int value) {
            inputValue = value;
        }

        public int getValue() {
            return inputValue;
        }

        protected int getDoubleValue() {
            return inputValue * 2;
        }

        int getTripleValue() {
            return inputValue * 3;
        }

        private int getQuadrupleValue() {
            return inputValue * 4;
        }

        public int callGetDoubleValue() {
            return getDoubleValue();
        }

        public int callGetTripleValue() {
            return getTripleValue();
        }

        public int callGetQuadrupleValue() {
            return getQuadrupleValue();
        }
    }

    private void makeCalls(ClassToRetransformObject testObj) {
        Assert.assertEquals(1, testObj.getValue());
        Assert.assertEquals(2, testObj.callGetDoubleValue());
        Assert.assertEquals(3, testObj.callGetTripleValue());
        Assert.assertEquals(4, testObj.callGetQuadrupleValue());
    }

    /**
     * Basic test which verifies that we can retransform public, protected, default, and private methods.
     * 
     * @throws Exception
     */
    @Test
    public void testClassRetransformPublicProtectedPrivateMethods() throws Exception {
        String txm1 = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getValue";
        String txm2 = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getDoubleValue";
        String txm3 = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getTripleValue";
        String txm4 = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getQuadrupleValue";

        // load class
        ClassToRetransformObject testObj = new ClassToRetransformObject(1);
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] {}, new String[] { txm1, txm2, txm3, txm4 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { txm1 }, new String[] { txm2, txm3, txm4 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getDoubleValue",
                "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { txm1, txm2 }, new String[] { txm3, txm4 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getTripleValue",
                "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { txm1, txm2, txm3 }, new String[] { txm4 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getQuadrupleValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { txm1, txm2, txm3, txm4 }, new String[] {});

    }
}
