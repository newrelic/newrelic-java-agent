/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.NoOpInstrumentation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseMatchTest {

    private Child child;

    @BeforeClass
    public static void setup() throws Exception {
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(BaseClass.class.getName(), Child.class.getName());
    }

    @Before
    public void before() {
        child = new Child();
    }

    /**
     * This test is to ensure that the inliner only inlines the correct places.
     */
    @Test
    public void testInliner() {
        Assert.assertEquals(2, child.constructorCounts);
        child.recursiveTest(4);
        Assert.assertEquals(8, child.recursiveCounts);
    }

    @Test
    public void test() {
        Assert.assertEquals("weaved child", child.childCall());
        Assert.assertEquals("weaved base", child.baseCall());
    }

    @Test
    public void testTrace() {
        final boolean[] success = { false };
        AgentBridge.instrumentation = new NoOpInstrumentation() {

            @Override
            public ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags, String instrumentationModule) {
                success[0] = true;
                return super.createTracer(invocationTarget, signatureId, metricName, flags, instrumentationModule);
            }

        };
        Assert.assertNull(child.justTrace());
        Assert.assertTrue(success[0]);
    }
}
