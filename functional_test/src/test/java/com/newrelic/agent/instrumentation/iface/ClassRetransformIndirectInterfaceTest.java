/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.iface;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import org.junit.Test;

import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

public class ClassRetransformIndirectInterfaceTest {

    /**
     * This tests reinstrumentation of a class which has an indirect interface.
     * 
     * @throws UnmodifiableClassException
     */
    @Test
    public void testIndirectInterface() throws UnmodifiableClassException {
        String transactionMetricLong = "OtherTransaction/Custom/" + SampleIndirectImplObject.class.getName()
                + "/getTestLong";
        String methodMetricLong = "Java/" + SampleIndirectImplObject.class.getName() + "/getTestLong";
        String transactionMetricInt = "OtherTransaction/Custom/" + SampleIndirectImplObject.class.getName()
                + "/getTestInt";
        String methodMetricInt = "Java/" + SampleIndirectImplObject.class.getName() + "/getTestInt";

        // load immediate interface class
        SampleInterfaceObject test = new SampleIndirectImplObject();
        test.getTestLong();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricLong);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleInterfaceObject.class.getName(),
                "getTestLong", "()J");

        test = new SampleIndirectImplObject();
        test.getTestLong();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricLong, 1);
        expected.put(methodMetricLong, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleInterfaceObject.class.getName(), "getTestInt",
                "()I");

        test = new SampleIndirectImplObject();
        test.getTestLong();
        test.getTestInt();
        test.getTestInt();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetricLong, 1);
        expected.put(methodMetricLong, 1);
        expected.put(transactionMetricInt, 2);
        expected.put(methodMetricInt, 2);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again - should not change anything
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleInterfaceObject.class.getName(), "getTestInt",
                "()I");

        test = new SampleIndirectImplObject();
        test.getTestLong();
        test.getTestLong();
        test.getTestInt();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetricLong, 2);
        expected.put(methodMetricLong, 2);
        expected.put(transactionMetricInt, 1);
        expected.put(methodMetricInt, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    @Test
    public void testInterfaceOfInterface() throws Exception {
        String transactionMetricOther = "OtherTransaction/Custom/" + SampleImplObject.class.getName() + "/getOtherNumber";
        String methodMetricOther = "Java/" + SampleImplObject.class.getName() + "/getOtherNumber";
        String transactionMetricChild = "OtherTransaction/Custom/" + SampleIndirectImplObject.class.getName()
                + "/getOtherNumber";
        String methodMetricChild = "Java/" + SampleIndirectImplObject.class.getName() + "/getOtherNumber";

        // load immediate interface class
        SampleInterfaceObject test = new SampleImplObject();
        test.getOtherNumber();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricOther);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleSuperInterfaceObject.class.getName(),
                "getOtherNumber", "()I");

        test = new SampleImplObject();
        test.getOtherNumber();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricOther, 1);
        expected.put(methodMetricOther, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again - should not change anything
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleSuperInterfaceObject.class.getName(),
                "getOtherNumber", "()I");

        test = new SampleImplObject();
        test.getTestLong();
        test.getTestInt();
        test.getTestLong();
        test.getOtherNumber();

        SampleInterfaceObject other = new SampleIndirectImplObject();
        other.getOtherNumber();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetricOther, 1);
        expected.put(methodMetricOther, 1);
        expected.put(transactionMetricChild, 1);
        expected.put(methodMetricChild, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);
    }

    @Test
    public void testInterfaceThroughAbstract() throws Exception {
        String transactionMetricYipee = "OtherTransaction/Custom/" + SampleAbstractImplObject.class.getName()
                + "/getTestIntYipee";
        String methodMetricYipee = "Java/" + SampleAbstractImplObject.class.getName() + "/getTestIntYipee";

        // load immediate interface class
        SampleInterfaceObject test = new SampleAbstractImplObject();
        test.getOtherNumber();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricYipee);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleSuperInterfaceObject.class.getName(),
                "getTestIntYipee", "()I");

        test.getTestIntYipee();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricYipee, 1);
        expected.put(methodMetricYipee, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again - should not change anything
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleSuperInterfaceObject.class.getName(),
                "getTestIntYipee", "()I");

        test = new SampleAbstractImplObject();
        test.getTestIntYipee();
        test.getTestInt();
        test.getTestIntYipee();
        test.getOtherNumber();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetricYipee, 2);
        expected.put(methodMetricYipee, 2);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);
    }

}
