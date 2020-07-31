/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StaticFieldTest {
    private StaticField test;

    @BeforeClass
    public static void setup() throws Exception {
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(VariableTest.class.getName(), StaticField.class.getName());
    }

    @Before
    public void before() {
        test = new StaticField();
    }

    @Test
    public void testStaticDoubleMemberVariable() {
        Assert.assertEquals(0d, test.staticDouble(5), 0);
        Assert.assertEquals(5d, test.staticDouble(66), 0);
        Assert.assertEquals(66d, test.staticDouble(-1), 0);
    }
}
