/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.testrunner;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.weavepackage.BaseTarget;
import com.example.weavepackage.ExactOriginal;
import com.example.weavepackage.InterfaceTarget;
import com.newrelic.agent.introspec.InstrumentationTestConfig;

@RunWith(WeavingTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.example.weavepackage")
public class WeavingTestRunnerTests {

    @Test
    public void testExactMatch() {
        assertEquals("weaved exact method", new ExactOriginal().exactMethod());
    }

    @Test
    public void testBaseMatch() {
        assertEquals("weaved abstract method", new BaseTarget().abstractMethod());
        assertEquals("weaved base method", new BaseTarget().baseMethod());
    }

    @Test
    public void testClassForNameReflection()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        BaseTarget base = (BaseTarget) Class.forName("com.example.weavepackage.BaseTarget").newInstance();
        assertEquals("weaved abstract method", base.abstractMethod());
        assertEquals("weaved base method", base.baseMethod());
    }

    @Test
    public void testInterfaceMatch() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        assertEquals("weaved interface method", new InterfaceTarget().interfaceMethod());
    }
}
