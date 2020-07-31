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

public class ConstructorTest {

    private TestConstructors testConstructors;

    @BeforeClass
    public static void setup() throws Exception {
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(TestConstructors.class.getName());
    }

    @Before
    public void before() {
        testConstructors = new TestConstructors();
    }

    @Test
    public void testVariableInit() {
        Assert.assertEquals("constructorInitOkay", testConstructors.variableInit());
        Assert.assertEquals("existingConstructorInitOkay", testConstructors.getExistingField());
    }

    @Test
    public void testConstructor() {
        testConstructors = new TestConstructors("test");
        Assert.assertEquals("test", testConstructors.variableInit());
    }
}
