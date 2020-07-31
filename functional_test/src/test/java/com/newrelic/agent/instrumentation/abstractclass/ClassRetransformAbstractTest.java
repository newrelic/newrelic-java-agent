/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.abstractclass;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.instrumentation.iface.SampleInterfaceObject;
import org.junit.Test;

import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

public class ClassRetransformAbstractTest {

    /**
     * This is instrumenting a method that is on an abstract class which is implemented in GenerateImplObject.
     */
    @Test
    public void testInstrumentingAbstractMethod() throws UnmodifiableClassException {
        String transactionMetric = "OtherTransaction/Custom/" + GeneratorImplObject.class.getName() + "/generateInt";
        String methodMetric = "Java/" + GeneratorImplObject.class.getName() + "/generateInt";

        // load immediate interface class
        GeneratorImplObject test = new GeneratorImplObject();
        test.generateInt();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformSuperclass(GeneratorAbstractObject.class.getName(),
                "generateInt", "()I");

        test = new GeneratorImplObject();
        test.generateInt();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleInterfaceObject.class.getName(),
                "generateInt", "()I");

        test = new GeneratorImplObject();
        test.generateInt();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    /**
     * This is instrumenting a method implemented on an abstract class and overridden on a child class.
     */
    @Test
    public void testMethodOnClassUsingSuperclass() throws UnmodifiableClassException {
        String transactionMetric = "OtherTransaction/Custom/" + GeneratorImplObject.class.getName() + "/performMagic";
        String methodMetric = "Java/" + GeneratorImplObject.class.getName() + "/performMagic";

        // load immediate interface class
        GeneratorImplObject test = new GeneratorImplObject();
        test.performMagic();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformSuperclass(GeneratorAbstractObject.class.getName(),
                "performMagic", "()V");

        test = new GeneratorImplObject();
        test.performMagic();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again
        InstrumentTestUtils.createTransformerAndRetransformSuperclass(GeneratorAbstractObject.class.getName(),
                "performMagic", "()V");

        test = new GeneratorImplObject();
        test.performMagic();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    /**
     * This is instrumenting a method implemented on an abstract class and not overridden on a child class.
     */
    @Test
    public void testMethodOnAbstractClassUsingSuperclass() throws UnmodifiableClassException {
        String transactionMetric = "OtherTransaction/Custom/" + GeneratorImplObject.class.getName() + "/undoMagic";
        String methodMetric = "Java/" + GeneratorImplObject.class.getName() + "/undoMagic";

        // load immediate interface class
        GeneratorImplObject test = new GeneratorImplObject();
        test.undoMagic();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformSuperclass(GeneratorAbstractObject.class.getName(),
                "undoMagic", "()V");

        test = new GeneratorImplObject();
        test.undoMagic();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again
        InstrumentTestUtils.createTransformerAndRetransformSuperclass(GeneratorAbstractObject.class.getName(),
                "undoMagic", "()V");

        test = new GeneratorImplObject();
        test.undoMagic();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    /**
     * Tests multiple levels of extension.
     */
    @Test
    public void testMultipleLevelsOfRefiement() throws UnmodifiableClassException {
        String transactionMetricThree = "OtherTransaction/Custom/" + ThreeObject.class.getName() + "/doSomeWork";
        String methodMetricThree = "Java/" + ThreeObject.class.getName() + "/doSomeWork";
        String transactionMetricFour = "OtherTransaction/Custom/" + ThreeObject.class.getName() + "/doSomeWork";
        String methodMetricFour = "Java/" + ThreeObject.class.getName() + "/doSomeWork";

        // load immediate interface class
        ThreeObject test = new ThreeObject();
        test.doSomeWork();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricThree);

        FourObject testFour = new FourObject();
        testFour.doSomeWork();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricFour);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformSuperclass(OneObject.class.getName(), "doSomeWork", "()V");

        test = new ThreeObject();
        test.doSomeWork();

        testFour.doSomeWork();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricThree, 1);
        expected.put(methodMetricThree, 1);
        expected.put(transactionMetricFour, 1);
        expected.put(methodMetricFour, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again - should not change
        InstrumentTestUtils.createTransformerAndRetransformSuperclass(OneObject.class.getName(), "doSomeWork", "()V");

        test = new ThreeObject();
        test.doSomeWork();

        testFour = new FourObject();
        testFour.doSomeWork();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetricThree, 1);
        expected.put(methodMetricThree, 1);
        expected.put(transactionMetricFour, 1);
        expected.put(methodMetricFour, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);
    }

    class OneObject {
        void doSomeWork() {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    class TwoObject extends OneObject {
        @Override
        void doSomeWork() {
            try {
                Thread.sleep(2);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    class ThreeObject extends TwoObject {
        @Override
        void doSomeWork() {
            try {
                Thread.sleep(3);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    class FourObject extends ThreeObject {
        @Override
        void doSomeWork() {
            try {
                Thread.sleep(4);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

}
