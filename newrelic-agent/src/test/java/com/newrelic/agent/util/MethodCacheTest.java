/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;

import org.junit.Test;

public class MethodCacheTest {

    @Test
    public void noArgs() throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException,
            IllegalAccessException {

        MethodCache methodCache = new MethodCache("getField1");
        String expectedField1 = "field1";
        Test1 test1 = new Test1(expectedField1);
        Class<?> clazz = test1.getClass();
        Method method1 = methodCache.getDeclaredMethod(clazz);
        method1.setAccessible(true);
        String actualField1 = (String) method1.invoke(test1);
        Assert.assertEquals(expectedField1, actualField1);
    }

    @Test
    public void args() throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException,
            IllegalAccessException {

        MethodCache methodCache = new MethodCache("getField1Args", Object.class);
        String expectedField1 = "field1";
        Test1 test1 = new Test1(expectedField1);
        Class<?> clazz = test1.getClass();
        Method method1 = methodCache.getDeclaredMethod(clazz);
        method1.setAccessible(true);
        String actualField1 = (String) method1.invoke(test1, new Object());
        Assert.assertEquals(expectedField1, actualField1);
    }

    @Test
    public void withParamTypes() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodCache methodCache = new MethodCache("getField1Args", Object.class);
        String expectedField1 = "field1";
        Test1 test1 = new Test1(expectedField1);
        Class<?> clazz = test1.getClass();
        Method method1 = methodCache.getDeclaredMethod(clazz, Object.class);
        method1.setAccessible(true);
        String actualField1 = (String) method1.invoke(test1, new Object());
        Assert.assertEquals(expectedField1, actualField1);
        Assert.assertEquals(methodCache.getSize(), 1);
    }

    @Test
    public void getMethodAndClear() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodCache methodCache = new MethodCache("getField1Args", Object.class);
        String expectedField1 = "field1";
        Test1 test1 = new Test1(expectedField1);
        Class<?> clazz = test1.getClass();
        Method method1 = methodCache.getMethod(clazz);
        method1.setAccessible(true);
        String actualField1 = (String) method1.invoke(test1, new Object());
        Assert.assertEquals(expectedField1, actualField1);
        Assert.assertEquals(methodCache.getSize(), 1);

        methodCache.clear();;
        Assert.assertEquals(methodCache.getSize(), 0);
    }

    @Test
    public void noArgsSuperclass() throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException,
            IllegalAccessException {

        MethodCache methodCache = new MethodCache("getField1");
        String expectedField1 = "field1";
        int expectedField2 = 1;
        // Test2 test2 = new Test2(expectedField1);
        // <?> clazz = test1.getClass();
        // Method method1 = methodCache.getDeclaredMethod(clazz);
        // method1.setAccessible(true);
        // String actualField1 = (String) method1.invoke(test1);
        // Assert.assertEquals(expectedField1, actualField1);
    }

    private static class Test1 {

        private String field1;

        public Test1(String field1) {
            this.field1 = field1;
        }

        public String getField1() {
            return field1;
        }

        public String getField1Args(Object arg1) {
            return field1;
        }

        public String getField1Args() {
            return field1;
        }

    }

    private static class Test2 extends Test1 {

        private int field2;

        public Test2(String field1, int field2) {
            super(field1);
            this.field2 = field2;
        }

        public int getField2() {
            return field2;
        }

        public int getField2Args(Object arg1) {
            return field2;
        }

    }

}
