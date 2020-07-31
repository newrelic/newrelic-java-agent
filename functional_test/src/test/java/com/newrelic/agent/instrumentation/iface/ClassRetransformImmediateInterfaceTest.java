/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.iface;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

public class ClassRetransformImmediateInterfaceTest {

    /**
     * This tests reinstrumentation of a class which has an immediate interface.
     * 
     * @throws UnmodifiableClassException
     */
    @Test
    public void testImmediateInterface() throws UnmodifiableClassException {
        String transactionMetricLong = "OtherTransaction/Custom/" + SampleImplObject.class.getName()
                + "/getTestLongWahoo";
        String methodMetricLong = "Java/" + SampleImplObject.class.getName() + "/getTestLongWahoo";
        String transactionMetricInt = "OtherTransaction/Custom/" + SampleImplObject.class.getName()
                + "/getTestIntWahoo";
        String methodMetricInt = "Java/" + SampleImplObject.class.getName() + "/getTestIntWahoo";

        // load immediate interface class
        SampleInterfaceObject test = new SampleImplObject();
        test.getTestLongWahoo();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricLong);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleInterfaceObject.class.getName(),
                "getTestLongWahoo", "()J");

        test = new SampleImplObject();
        test.getTestLongWahoo();

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricLong, 1);
        expected.put(methodMetricLong, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(SampleInterfaceObject.class.getName(),
                "getTestIntWahoo", "()I");

        test = new SampleImplObject();
        test.getTestLongWahoo();
        test.getTestIntWahoo();
        test.getTestIntWahoo();

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetricLong, 1);
        expected.put(methodMetricLong, 1);
        expected.put(transactionMetricInt, 2);
        expected.put(methodMetricInt, 2);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    @Test
    public void testCoreJdkInterface() throws Exception {
        String transactionMetric = "OtherTransaction/Custom/" + Line2D.Double.class.getName() + "/contains";
        String methodMetric = "Java/" + Line2D.Double.class.getName() + "/contains";
        String transactionMetricPath = "OtherTransaction/Custom/" + Path2D.Double.class.getName() + "/contains";
        String methodMetricPath = "Java/" + Path2D.Double.class.getName() + "/contains";

        // load immediate interface class
        Line2D.Double test = new Line2D.Double();
        test.contains(1, 5);
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        Path2D.Double path = new Path2D.Double();
        path.contains(5, 6);
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricPath);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(Shape.class.getName(), "contains", "(DD)Z");

        test = new Line2D.Double();
        test.contains(4, 6);

        path.contains(5, 6);
        path.contains(1, 8);

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        expected.put(transactionMetricPath, 2);
        expected.put(methodMetricPath, 2);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument - again
        InstrumentTestUtils.createTransformerAndRetransformInterface(Shape.class.getName(), "contains", "(DD)Z");

        test = new Line2D.Double();
        test.contains(3, 9);

        // verify metrics
        expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }
}
