/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import java.util.List;

import com.newrelic.agent.util.AgentError;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.commons.GeneratorAdapter;

public class BytecodeGenProxyBuilderTest {

    @Test
    public void test() {
        GeneratorAdapter methodAdapter = Mockito.mock(GeneratorAdapter.class);
        List list = BytecodeGenProxyBuilder.newBuilder(List.class, methodAdapter, true).build();

        // just verify that this doesn't blow up. Primitive return values were causing NPEs at one point
        list.add("test");
        list.indexOf("test");
    }

    @Test
    public void test_variables() {
        GeneratorAdapter methodAdapter = Mockito.mock(GeneratorAdapter.class);
        BytecodeGenProxyBuilder<TestClass> builder = BytecodeGenProxyBuilder.newBuilder(TestClass.class, methodAdapter, true);
        Variables variables = builder.getVariables();
        Assert.assertNotNull(variables);
    }

    @Test
    public void test_supportedTypes() {
        GeneratorAdapter methodAdapter = Mockito.mock(GeneratorAdapter.class);
        BytecodeGenProxyBuilder<TestClass> builder = BytecodeGenProxyBuilder.newBuilder(TestClass.class, methodAdapter, true);
        TestClass testClass = builder.build();
        testClass.methodWithAllTypes("s", true, 1, 2L, (float)3.0, 4.4, (byte)5, new int[]{6, 7});
    }

    @Test (expected = AgentError.class)
    public void test_unsupportedType() {
        GeneratorAdapter methodAdapter = Mockito.mock(GeneratorAdapter.class);
        BytecodeGenProxyBuilder<TestClassUnsupportedType> builder = BytecodeGenProxyBuilder.newBuilder(TestClassUnsupportedType.class, methodAdapter, true);
        TestClassUnsupportedType testClass = builder.build();
        testClass.methodWithAllTypes(new Object());
    }

    private interface TestClass {
        int methodWithAllTypes(String o, boolean b, int i, long l, float f, double d, byte b2, int[] ints);
    }


    private interface TestClassUnsupportedType {
        int methodWithAllTypes(Object o);
    }
}
