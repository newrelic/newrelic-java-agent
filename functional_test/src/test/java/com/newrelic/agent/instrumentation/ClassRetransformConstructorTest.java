/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import org.junit.Assert;

import org.junit.Test;

public class ClassRetransformConstructorTest {

    public abstract static class SuperClassToRetransformObject {
        protected int val;

        public SuperClassToRetransformObject(int val) {
            this.val = val;
        }
    }

    public static class ClassToRetransformObject extends SuperClassToRetransformObject {

        public ClassToRetransformObject(int val) {
            super(val);
        }

        public int getVal() {
            return val;
        }
    }

    public abstract static class SuperClassToRetransform2Object {
        protected int val;

        public SuperClassToRetransform2Object(int val) {
            this.val = val;
        }
    }

    public static class ClassToRetransform2Object extends SuperClassToRetransform2Object {

        public ClassToRetransform2Object(int val) {
            super(val);
        }

        public int getVal() {
            return val;
        }
    }

    public class ClassToRetransform3Object {
        protected int val;

        public ClassToRetransform3Object(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    @Test
    public void testClassRetransformSuperConstructor() throws Exception {
        String methodMetric = "Java/" + ClassToRetransformObject.class.getName() + "/<init>";

        // load class
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());
        InstrumentTestUtils.verifyMetricNotPresent(methodMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(SuperClassToRetransformObject.class.getName(), "<init>",
                "(I)V");

        // call constructor on class that was transformed
        Assert.assertEquals(1, new ClassToRetransformObject(1).getVal());

        InstrumentTestUtils.verifyMetricPresent(methodMetric);

    }

    @Test
    public void testClassRetransformConstructor() throws Exception {
        String methodMetric = "Java/" + ClassToRetransform2Object.class.getName() + "/<init>";

        // load class
        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());
        InstrumentTestUtils.verifyMetricNotPresent(methodMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform2Object.class.getName(), "<init>", "(I)V");

        // call constructor on class that was transformed
        Assert.assertEquals(1, new ClassToRetransform2Object(1).getVal());

        InstrumentTestUtils.verifyMetricPresent(methodMetric);
    }

    @Test
    public void testNonStaticClassRetransformConstructor() throws Exception {
        String methodMetric = "Java/" + ClassToRetransform3Object.class.getName() + "/<init>";

        // load class
        Assert.assertEquals(1, new ClassToRetransform3Object(1).getVal());
        InstrumentTestUtils.verifyMetricNotPresent(methodMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransform3Object.class.getName(), "<init>",
                "(Lcom/newrelic/agent/instrumentation/ClassRetransformConstructorTest;I)V");

        // call constructor on class that was transformed
        Assert.assertEquals(1, new ClassToRetransform3Object(1).getVal());

        InstrumentTestUtils.verifyMetricPresent(methodMetric);
    }

}
