/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.serialization;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

public class ClassRetransformSerializationTest {

    @Test
    public void testBasicSerialization() throws UnmodifiableClassException {
        String transactionMetricSerialVersion = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialVersionObject" + "/theSerialWork";
        String methodMetricSerialVersion = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialVersionObject" + "/theSerialWork";
        String transactionMetricNoSerialVersion = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialVersionObject" + "/theNoSerialWork";
        String methodMetricNoSerialVersion = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialVersionObject" + "/theNoSerialWork";

        WithSerialVersionObject message = new WithSerialVersionObject();
        message.theSerialWork();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricSerialVersion);

        NoSerialVersionObject none = new NoSerialVersionObject();
        none.theNoSerialWork();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricNoSerialVersion);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(WithSerialVersionObject.class.getName(),
                "theSerialWork", "()V");

        // call method on class that was transformed
        message = new WithSerialVersionObject();
        message.theSerialWork();

        none = new NoSerialVersionObject();
        none.theNoSerialWork();

        // // verify
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricSerialVersion, 1);
        expected.put(methodMetricSerialVersion, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(NoSerialVersionObject.class.getName(),
                "theNoSerialWork", "()V");

        // call method on class that was transformed
        message = new WithSerialVersionObject();
        message.theSerialWork();

        none = new NoSerialVersionObject();
        none.theNoSerialWork();

        // verify
        expected = new HashMap<>();
        expected.put(transactionMetricSerialVersion, 1);
        expected.put(methodMetricSerialVersion, 1);
        expected.put(transactionMetricNoSerialVersion, 1);
        expected.put(methodMetricNoSerialVersion, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);
    }

    @Test
    public void testActualSerialization() throws UnmodifiableClassException {
        String transactionMetricSerialVersion = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialVersionObject" + "/theSerialOther";
        String methodMetricSerialVersion = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialVersionObject" + "/theSerialOther";
        String transactionMetricNoSerialVersion = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialVersionObject" + "/theNoSerialOther";
        String methodMetricNoSerialVersion = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialVersionObject" + "/theNoSerialOther";

        WithSerialVersionObject message = (WithSerialVersionObject) serializeClass(new WithSerialVersionObject());
        message.theSerialWork();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricSerialVersion);

        NoSerialVersionObject none = (NoSerialVersionObject) serializeClass(new NoSerialVersionObject());
        none.theNoSerialWork();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetricNoSerialVersion);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(WithSerialVersionObject.class.getName(),
                "theSerialOther", "()V");

        // call method on class that was transformed
        message = (WithSerialVersionObject) serializeClass(new WithSerialVersionObject());
        message.theSerialOther();
        int serialValue = message.getValue();

        none = (NoSerialVersionObject) serializeClass(new NoSerialVersionObject());
        none.theNoSerialOther();
        int noValue = none.getValue();

        // // verify
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricSerialVersion, 1);
        expected.put(methodMetricSerialVersion, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(NoSerialVersionObject.class.getName(),
                "theNoSerialOther", "()V");

        // call method on class that was transformed
        message = (WithSerialVersionObject) serializeClass(new WithSerialVersionObject());
        message.theSerialOther();
        Assert.assertEquals(serialValue, message.getValue());

        none = (NoSerialVersionObject) serializeClass(new NoSerialVersionObject());
        none.theNoSerialOther();
        Assert.assertEquals(noValue, none.getValue());

        // verify
        expected = new HashMap<>();
        expected.put(transactionMetricSerialVersion, 1);
        expected.put(methodMetricSerialVersion, 1);
        expected.put(transactionMetricNoSerialVersion, 1);
        expected.put(methodMetricNoSerialVersion, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);
    }

    @Test
    public void testStartupSerializationWithValue() throws UnmodifiableClassException {
        String transactionMetricSerialVersion = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialMatchStartupObject" + "/getDoubleValue";
        String methodMetricSerialVersion = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialMatchStartupObject" + "/getDoubleValue";
        String transactionMetricGetValue = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialMatchStartupObject" + "/getValue";
        String methodMetricGetValue = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.WithSerialMatchStartupObject" + "/getValue";

        WithSerialMatchStartupObject first = new WithSerialMatchStartupObject();
        int initValue = first.getDoubleValue();

        WithSerialMatchStartupObject message = (WithSerialMatchStartupObject) serializeClass(first);
        int afterValue = message.getDoubleValue();
        Assert.assertEquals(initValue, afterValue);

        // verify
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricSerialVersion, 2);
        expected.put(methodMetricSerialVersion, 2);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(WithSerialMatchStartupObject.class.getName(),
                "getValue", "()I");

        // call method on class that was transformed
        message = (WithSerialMatchStartupObject) serializeClass(new WithSerialMatchStartupObject());
        Assert.assertEquals(initValue, message.getDoubleValue());
        Assert.assertEquals(initValue, message.getValue() * 2);

        // verify
        expected = new HashMap<>();
        expected.put(transactionMetricSerialVersion, 1);
        expected.put(methodMetricSerialVersion, 1);
        expected.put(transactionMetricGetValue, 1);
        expected.put(methodMetricGetValue, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);
    }

    @Test
    public void testStartupSerializationNoValue() throws UnmodifiableClassException {
        String transactionMetricGetDouble = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialMatchStartupObject" + "/getDoubleValue";
        String methodMetricGetDouble = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialMatchStartupObject" + "/getDoubleValue";
        String transactionMetricGetValue = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialMatchStartupObject" + "/getValue";
        String methodMetricGetValue = "Java/"
                + "com.newrelic.agent.instrumentation.serialization.NoSerialMatchStartupObject" + "/getValue";

        NoSerialMatchStartupObject first = new NoSerialMatchStartupObject();
        int initValue = first.getDoubleValue();

        NoSerialMatchStartupObject message = (NoSerialMatchStartupObject) serializeClass(first);
        int afterValue = message.getDoubleValue();
        Assert.assertEquals(initValue, afterValue);

        // verify
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetricGetDouble, 2);
        expected.put(methodMetricGetDouble, 2);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformClass(NoSerialMatchStartupObject.class.getName(),
                "getValue", "()I");

        // call method on class that was transformed
        message = (NoSerialMatchStartupObject) serializeClass(new NoSerialMatchStartupObject());
        Assert.assertEquals(initValue, message.getDoubleValue());
        Assert.assertEquals(initValue, message.getValue() * 2);

        // verify
        expected = new HashMap<>();
        expected.put(transactionMetricGetDouble, 1);
        expected.put(methodMetricGetDouble, 1);
        expected.put(transactionMetricGetValue, 1);
        expected.put(methodMetricGetValue, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);
    }

    private static Object serializeClass(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();

            byte[] theBytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(theBytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Was unable to serialize class " + obj.getClass().getName() + " Message: " + e.getMessage());
            // never actually going to get here
            return null;
        }
    }

}
