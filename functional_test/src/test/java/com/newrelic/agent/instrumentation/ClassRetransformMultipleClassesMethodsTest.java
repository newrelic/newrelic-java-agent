/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Assert;

import org.junit.Test;

public class ClassRetransformMultipleClassesMethodsTest {

    static class ClassToRetransformObject {

        private int inputValue;

        public ClassToRetransformObject(int value) {
            inputValue = value;
        }

        public int getValue() {
            return inputValue;
        }
    }

    static class ClassToRetransform2Object {

        private int inputValue;

        public ClassToRetransform2Object(int value) {
            inputValue = value;
        }

        public int getValue() {
            return inputValue;
        }
    }

    static class ClassToRetransform3Object {

        private int inputValue;

        public ClassToRetransform3Object(int value) {
            inputValue = value;
        }

        public int getValue() {
            return inputValue;
        }

        public int getDoubleValue() {
            return inputValue * 2;
        }
    }

    /**
     * Basic test which verifies that we can retransform a constructor.
     * 
     * @throws Exception
     */
    @Test
    public void testInstrumentMultipleClasses() throws Exception {
        String transactionMetric = "OtherTransaction/Custom/" + ClassToRetransformObject.class.getName() + "/getValue";
        String transactionMetric2 = "OtherTransaction/Custom/" + ClassToRetransform2Object.class.getName() + "/getValue";

        // load classes
        ClassToRetransformObject class1 = new ClassToRetransformObject(1);
        ClassToRetransform2Object class2 = new ClassToRetransform2Object(2);
        Assert.assertEquals(1, class1.getValue());
        Assert.assertEquals(2, class2.getValue());
        InstrumentTestUtils.verifyMetrics(new String[] {}, new String[] { transactionMetric, transactionMetric2 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(), "getValue", "()I");

        // call method on class that was transformed
        Assert.assertEquals(1, class1.getValue());
        Assert.assertEquals(2, class2.getValue());
        InstrumentTestUtils.verifyMetrics(new String[] { transactionMetric }, new String[] { transactionMetric2 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform2Object.class.getName(), "getValue", "()I");

        // call method on class that was transformed
        Assert.assertEquals(1, class1.getValue());
        Assert.assertEquals(2, class2.getValue());
        InstrumentTestUtils.verifyMetrics(new String[] { transactionMetric, transactionMetric2 }, new String[] {});
    }

    @Test
    public void testInstrumentMultipleMethods() throws Exception {
        String transactionMetric = "OtherTransaction/Custom/" + ClassToRetransform3Object.class.getName() + "/getValue";
        String transactionMetric2 = "OtherTransaction/Custom/" + ClassToRetransform3Object.class.getName()
                + "/getDoubleValue";

        // load classes
        ClassToRetransform3Object testObject = new ClassToRetransform3Object(1);
        Assert.assertEquals(1, testObject.getValue());
        Assert.assertEquals(2, testObject.getDoubleValue());
        InstrumentTestUtils.verifyMetrics(new String[] {}, new String[] { transactionMetric, transactionMetric2 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform3Object.class.getName(), "getValue", "()I");

        // call method on class that was transformed
        Assert.assertEquals(1, testObject.getValue());
        Assert.assertEquals(2, testObject.getDoubleValue());
        InstrumentTestUtils.verifyMetrics(new String[] { transactionMetric }, new String[] { transactionMetric2 });

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform3Object.class.getName(), "getDoubleValue",
                "()I");

        // call method on class that was transformed
        Assert.assertEquals(1, testObject.getValue());
        Assert.assertEquals(2, testObject.getDoubleValue());
        InstrumentTestUtils.verifyMetrics(new String[] { transactionMetric, transactionMetric2 }, new String[] {});
    }
}
