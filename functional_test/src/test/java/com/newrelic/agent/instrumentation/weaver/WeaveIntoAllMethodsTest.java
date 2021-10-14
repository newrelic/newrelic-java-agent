/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class WeaveIntoAllMethodsTest {
    private static Class<?> test;

    @BeforeClass
    public static void setup() throws Exception {
        // Load weave module first. Changing the order defeats the purpose of this test.
        FunctionalWeaveTestUtils.reinstrument();
        test = WeaveIntoAllMethodsTest.class.getClassLoader().loadClass("com.newrelic.agent.instrumentation.weaver.WeaveIntoAllMethods");
    }

    @Test
    public void testWeaveIntoAllMethods() throws Exception {
        // The class we're using to test was compiled with Java 8's flag -parameters. I couldn't figure out how
        // to compile functional tests with -parameters, hence loading via reflection.
        Object o = test.newInstance();

        Class[] noParameters = {};
        Method callAllMethods = test.getDeclaredMethod("callAllMethods", noParameters);
        callAllMethods.invoke(o, (Object[]) noParameters);

        Field callCount = test.getDeclaredField("callCount");

        assertEquals(4, callCount.getInt(o));
    }

    @Test
    public void testMalformedParametersException() throws ClassNotFoundException {
        // JAVA-2770
        Method[] methods = test.getMethods();
        methods[0].getParameters();
        methods[1].getParameters();
        methods[2].getParameters();
        methods[3].getParameters();
        methods[4].getParameters();
    }
}
