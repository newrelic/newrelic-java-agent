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

public class ClassRetransformAbstractSuperclassMethodsTest {

    abstract static class SuperClassToRetransformObject {

        protected int inputValue;

        public SuperClassToRetransformObject(int value) {
            inputValue = value;
        }

        public int getStraightSuperValue() {
            return inputValue;
        }

        public int getSuperValue() {
            return inputValue * 2;
        }

        public int getOverriddenValue() {
            return inputValue * -3;
        }

        public abstract int getAbstractValue();

        public int getAbstractValueIndirect() {
            return getAbstractValue() + 1;
        }

        public int getIndirectSuperValue() {
            return inputValue * 6;
        }
    }

    static class ClassToRetransformObject extends SuperClassToRetransformObject {

        public ClassToRetransformObject(int value) {
            super(value);
        }

        @Override
        public int getSuperValue() {
            return super.getSuperValue();
        }

        @Override
        public int getOverriddenValue() {
            return inputValue * 3;
        }

        @Override
        public int getAbstractValue() {
            return inputValue * 4;
        }

        public int getIndirectValue() {
            return super.getIndirectSuperValue();
        }
    }

    private void makeCalls(ClassToRetransformObject testObj) {
        Assert.assertEquals(1, testObj.getStraightSuperValue());
        Assert.assertEquals(2, testObj.getSuperValue());
        Assert.assertEquals(3, testObj.getOverriddenValue());
        Assert.assertEquals(4, testObj.getAbstractValue());
        Assert.assertEquals(5, testObj.getAbstractValueIndirect());
        Assert.assertEquals(6, testObj.getIndirectValue());
    }

    public static String straightSuperValue = "Java/" + ClassToRetransformObject.class.getName()
            + "/getStraightSuperValue";
    public static String superValue = "Java/" + ClassToRetransformObject.class.getName() + "/getSuperValue";
    public static String overriddenValue = "Java/" + ClassToRetransformObject.class.getName() + "/getOverriddenValue";
    public static String abstractValue = "Java/" + ClassToRetransformObject.class.getName() + "/getAbstractValue";
    public static String abstractValueIndirect = "Java/" + ClassToRetransformObject.class.getName()
            + "/getAbstractValueIndirect";
    public static String indirectSuperValue = "Java/" + ClassToRetransformObject.class.getName()
            + "/getIndirectSuperValue";
    public static String indirectValue = "Java/" + ClassToRetransformObject.class.getName() + "/getIndirectValue";

    @Test
    public void testClassRetransformSuperclassMethods() throws Exception {

        ClassToRetransformObject testObj = new ClassToRetransformObject(1);
        makeCalls(testObj);
        // Nothing Instrumented
        InstrumentTestUtils.verifyMetrics(new String[] {}, new String[] { straightSuperValue, superValue,
                overriddenValue, abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });

        // Try to instrument nonexistent method in child
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getStraightSuperValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] {}, new String[] { straightSuperValue, superValue,
                overriddenValue, abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });

        // Instrument method only found in superclass
        InstrumentTestUtils.createTransformerAndRetransformClass(SuperClassToRetransformObject.class.getName(),
                "getStraightSuperValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue }, new String[] { superValue,
                overriddenValue, abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });

        // Instrument overridden but indirectly called method
        InstrumentTestUtils.createTransformerAndRetransformClass(SuperClassToRetransformObject.class.getName(),
                "getSuperValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue }, new String[] {
                overriddenValue, abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });

        // Instrument overridden method
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getSuperValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue }, new String[] {
                overriddenValue, abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });
        makeCalls(testObj);
        Map<String, Integer> expected = new HashMap<>();
        expected.put(superValue, 2);
        InstrumentTestUtils.verifyCountMetric(expected);

        // Instrument super's overridden method that is never called
        InstrumentTestUtils.createTransformerAndRetransformClass(SuperClassToRetransformObject.class.getName(),
                "getOverriddenValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue }, new String[] {
                overriddenValue, abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });

        // Instrument subclass' overridden method that is never called
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getOverriddenValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue, overriddenValue },
                new String[] { abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });

        // Instrument nonexistent method in subclass
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getAbstractValueIndirect", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue, overriddenValue },
                new String[] { abstractValue, abstractValueIndirect, indirectSuperValue, indirectValue });

        // Instrument superclass' method that calls abstract method
        InstrumentTestUtils.createTransformerAndRetransformClass(SuperClassToRetransformObject.class.getName(),
                "getAbstractValueIndirect", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue, overriddenValue,
                abstractValueIndirect }, new String[] { abstractValue, indirectSuperValue, indirectValue });

        // Instrument abstract method on superclass which does nothing
        InstrumentTestUtils.createTransformerAndRetransformClass(SuperClassToRetransformObject.class.getName(),
                "getAbstractValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue, overriddenValue,
                abstractValueIndirect }, new String[] { abstractValue, indirectSuperValue, indirectValue });

        // Instrument abstract method on subclass
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getAbstractValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue, overriddenValue,
                abstractValueIndirect, abstractValue }, new String[] { indirectSuperValue, indirectValue });

        // Instrument subclass method that calls superclass method of different name
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getIndirectValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue, overriddenValue,
                abstractValueIndirect, abstractValue, indirectValue }, new String[] { indirectSuperValue });

        // Instrument indirectly called method in superclass
        InstrumentTestUtils.createTransformerAndRetransformClass(ClassToRetransformObject.class.getName(),
                "getIndirectSuperValue", "()I");
        makeCalls(testObj);
        InstrumentTestUtils.verifyMetrics(new String[] { straightSuperValue, superValue, overriddenValue,
                abstractValueIndirect, abstractValue, indirectValue }, new String[] { indirectSuperValue });

    }

}
