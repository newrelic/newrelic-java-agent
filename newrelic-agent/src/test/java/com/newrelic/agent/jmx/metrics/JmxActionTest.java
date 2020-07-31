/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JmxActionTest {

    @Test
    public void testUseFirstAtt() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 5f);
        values.put("goodbye", 7f);
        values.put("work", 9f);
        String[] atts = new String[] { "hello" };

        Assert.assertEquals(5f, JmxAction.USE_FIRST_ATT.performAction(atts, values), .001);

        atts = new String[] { "work" };

        Assert.assertEquals(9f, JmxAction.USE_FIRST_ATT.performAction(atts, values), .001);
    }

    @Test
    public void testSubtractAllFromFirst1() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 10f);
        values.put("goodbye", 7f);
        values.put("work", 9f);
        String[] atts = new String[] { "hello", "goodbye" };

        Assert.assertEquals(3f, JmxAction.SUBTRACT_ALL_FROM_FIRST.performAction(atts, values), .001);

        atts = new String[] { "work" };

        Assert.assertEquals(9f, JmxAction.SUBTRACT_ALL_FROM_FIRST.performAction(atts, values), .001);
    }

    @Test
    public void testSubtractAllFromFirst2() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 15f);
        values.put("goodbye", 7f);
        values.put("work", 2f);
        String[] atts = new String[] { "hello", "goodbye", "work" };

        Assert.assertEquals(6f, JmxAction.SUBTRACT_ALL_FROM_FIRST.performAction(atts, values), .001);
    }

    @Test
    public void testSumAll1() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 10f);
        values.put("goodbye", 7f);
        values.put("work", 9f);
        String[] atts = new String[] { "hello", "goodbye" };

        Assert.assertEquals(17f, JmxAction.SUM_ALL.performAction(atts, values), .001);

        atts = new String[] { "work" };

        Assert.assertEquals(9f, JmxAction.SUM_ALL.performAction(atts, values), .001);
    }

    @Test
    public void testSumAll2() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 15f);
        values.put("goodbye", 7f);
        values.put("work", 2f);
        String[] atts = new String[] { "hello", "goodbye", "work" };

        Assert.assertEquals(24f, JmxAction.SUM_ALL.performAction(atts, values), .001);
    }

    @Test
    public void testUseFirstRecordedAtt1() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 15f);
        values.put("goodbye", 7f);
        values.put("work", 2f);
        String[] atts = new String[] { "hello", "goodbye", "work" };

        Assert.assertEquals(15f, JmxAction.USE_FIRST_RECORDED_ATT.performAction(atts, values), .001);
    }

    @Test
    public void testUseFirstRecordedAtt2() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 2f);
        values.put("Hello", 7f);
        String[] atts = new String[] { "hello", "Hello" };

        Assert.assertEquals(2f, JmxAction.USE_FIRST_RECORDED_ATT.performAction(atts, values), .001);
    }

    @Test
    public void testUseFirstRecordedAtt3() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 3f);
        String[] atts = new String[] { "hello" };

        Assert.assertEquals(3f, JmxAction.USE_FIRST_RECORDED_ATT.performAction(atts, values), .001);
    }

    @Test
    public void testUseFirstRecordedAtt4() {
        Map<String, Float> values = new HashMap<>();
        String[] atts = new String[] {};

        Assert.assertEquals(0f, JmxAction.USE_FIRST_RECORDED_ATT.performAction(atts, values), .001);
    }

    @Test
    public void testUseFirstRecordedAtt5() {
        Map<String, Float> values = new HashMap<>();
        values.put("hello", 3f);
        String[] atts = new String[] { "wrongOne" };

        Assert.assertEquals(0f, JmxAction.USE_FIRST_RECORDED_ATT.performAction(atts, values), .001);
    }

    @Test
    public void testUseFirstRecordedAtt6() {
        Map<String, Float> values = new HashMap<>();
        String[] atts = new String[] { "wrongOne" };

        Assert.assertEquals(0f, JmxAction.USE_FIRST_RECORDED_ATT.performAction(atts, values), .001);
    }

}
