/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.method;

import com.newrelic.agent.profile.TestFileWithLineNumbers;
import com.newrelic.agent.profile.v2.Murmur3StringMap;
import com.newrelic.agent.util.StringMap;
import com.newrelic.agent.util.asm.ClassStructure;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodInfoTest {

    @Test
    public void methodNotPresent() {
        MethodInfo info = MethodInfoUtil.createMethodInfo(TestFileWithLineNumbers.class, "notPresent", -1);
        Assert.assertNotNull(info);
        List<Map<String, Object>> actual = info.getJsonMethodMaps();
        Assert.assertNotNull(actual);
        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void testCreateMethodInfoForConstructorsNoLineNumbers() {
        verifyAndGetArgsForConstructorsNoLineNumbers(new TestFileWithLineNumbers());

        verifyAndGetArgsForConstructorsNoLineNumbers(new TestFileWithLineNumbers(1, 2L));

        verifyAndGetArgsForConstructorsNoLineNumbers(new TestFileWithLineNumbers(new int[0], new byte[0][0],
                new String[0][0][0]));

        verifyAndGetArgsForConstructorsNoLineNumbers(new TestFileWithLineNumbers(new Object(),
                new ArrayList<String>(), "1234", new HashSet<>()));

    }

    @Test
    public void getJsonMethodMaps() {
        MethodInfo info = MethodInfoUtil.createMethodInfo(TestFileWithLineNumbers.class, "instrumentedMethod", -1);
        StringMap stringMap = new Murmur3StringMap();
        stringMap.addString("instrumentedMethod");
        List<Map<String, Object>> results = info.getJsonMethodMaps(stringMap);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
    }

    private void verifyAndGetArgsForConstructorsNoLineNumbers(TestFileWithLineNumbers newClass) {

        StackTraceElement trace = getTraceElementForConstructor(newClass.getTrace());
        MethodInfo info = MethodInfoUtil.createMethodInfo(TestFileWithLineNumbers.class, trace.getMethodName(), -1);
        List<Map<String, Object>> methods = verifyBasicsNoLineNumber(info);
        Assert.assertEquals(4, methods.size());
        int[] methodArgSizes = new int[] { -1, -1, -1, -1, -1 };
        for (Map<String, Object> current : methods) {
            List<String> args = (List<String>) current.get("args");
            int size = args.size();
            methodArgSizes[size] = size;

            if (size == 2) {
                Assert.assertEquals("int", args.get(0));
                Assert.assertEquals("long", args.get(1));
            } else if (size == 3) {
                Assert.assertEquals("int[]", args.get(0));
                Assert.assertEquals("byte[][]", args.get(1));
                Assert.assertEquals("java.lang.String[][][]", args.get(2));
            } else if (size == 4) {
                Assert.assertEquals("java.lang.Object", args.get(0));
                Assert.assertEquals("java.util.List", args.get(1));
                Assert.assertEquals("java.lang.String", args.get(2));
                Assert.assertEquals("java.util.Set", args.get(3));
            }
        }
        Assert.assertArrayEquals(new int[] { 0, -1, 2, 3, 4 }, methodArgSizes);
    }

    private static StackTraceElement getTraceElementForConstructor(StackTraceElement[] elements) {
        for (StackTraceElement current : elements) {
            if (current.getMethodName().equals("<init>")) {
                return current;
            }
        }
        return null;
    }

    @Test
    public void testCreateMethodInfoForMethodsNoLineNumbers() {
        TestFileWithLineNumbers test = new TestFileWithLineNumbers();

        verifyExactAndGetArgsForMethodsNoLineNumbers(test.foo());

        verifyExactAndGetArgsForMethodsNoLineNumbers(test.foo(Integer.valueOf(1), Integer.valueOf(2)));

        verifyExactAndGetArgsForMethodsNoLineNumbers(test.foo(Short.parseShort("1"), Byte.parseByte("0"), new int[0],
                new ArrayList<String>(), new HashMap<String, String>()));

        verifyExactAndGetArgsForMethodsNoLineNumbers(test.foo(new Integer[0][0], new Object[0]));
    }

    private void verifyExactAndGetArgsForMethodsNoLineNumbers(StackTraceElement[] element) {

        StackTraceElement trace = getTraceElementForFoo(element);
        MethodInfo info = MethodInfoUtil.createMethodInfo(TestFileWithLineNumbers.class, trace.getMethodName(), -1);
        List<Map<String, Object>> methods = verifyBasicsNoLineNumber(info);
        Assert.assertEquals(8, methods.size());
        int[] sizes = new int[8];
        int count = 0;
        for (Map<String, Object> current : methods) {
            List<String> args = (List<String>) current.get("args");
            sizes[count] = args.size();
            count++;

            int size = args.size();
            if (size == 1) {
                Assert.assertEquals("java.lang.String", args.get(0));
            } else if (size == 5) {
                Assert.assertEquals("short", args.get(0));
                Assert.assertEquals("byte", args.get(1));
                Assert.assertEquals("int[]", args.get(2));
                Assert.assertEquals("java.util.List", args.get(3));
                Assert.assertEquals("java.util.Map", args.get(4));
            }
        }
        Arrays.sort(sizes);
        Assert.assertEquals(0, sizes[0]);
        Assert.assertEquals(1, sizes[1]);
        Assert.assertEquals(2, sizes[2]);
        Assert.assertEquals(2, sizes[3]);
        Assert.assertEquals(2, sizes[4]);
        Assert.assertEquals(2, sizes[5]);
        Assert.assertEquals(2, sizes[6]);
        Assert.assertEquals(5, sizes[7]);
    }

    private static List<Map<String, Object>> verifyBasicsNoLineNumber(MethodInfo info) {
        Assert.assertNotNull(info);
        Assert.assertTrue(info instanceof MultipleMethodInfo);
        List<Map<String, Object>> actual = info.getJsonMethodMaps();
        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.size() >= 1);
        return actual;
    }

    private static StackTraceElement getTraceElementForFoo(StackTraceElement[] elements) {
        for (StackTraceElement current : elements) {
            if (current.getMethodName().equals("foo")) {
                return current;
            }
        }
        return null;
    }

    @Test
    public void getClassNameNullCanonicalName() throws ClassNotFoundException {

        final Object anonymousInnerClass = new Object() {
        };

        // The CanonicalName of the anonymous inner class is null so we fall back to the Type name
        Assert.assertEquals("com.newrelic.agent.profile.method.MethodInfoTest$1", MethodInfoUtil.getClassName(anonymousInnerClass.getClass()));
    }

    @Test
    public void testGetMethodInfo() {
        class ClassWithStuffa {
            private final long myField;

            public ClassWithStuffa() {
                myField = 0;
            }

            public long getMyField() {
                return myField;
            }
        }
        // jacoco coverage inserts itself into our tests causing conflicts in these assertions.
        Method[] m = filterJacocoMethods(ClassWithStuffa.class.getDeclaredMethods());
        Assert.assertEquals(1, m.length);

        MethodInfo actual = MethodInfoUtil.getMethodInfo(ClassWithStuffa.class, m[0].getName(), "()V");
        Assert.assertNotNull(actual);
        List<Map<String, Object>> secondActual = actual.getJsonMethodMaps();
        Assert.assertNotNull(secondActual);
        Assert.assertEquals(1, secondActual.size());
        Map<String, Object> methodData = secondActual.get(0);
        Assert.assertNotNull(methodData);
        Assert.assertEquals(1, methodData.size());
        List<String> args = (List<String>) methodData.get("args");
        Assert.assertNotNull(args);
        Assert.assertEquals(0, args.size());

        Constructor[] c = ClassWithStuffa.class.getConstructors();
        Assert.assertEquals(1, c.length);
        MethodInfo constructor = MethodInfoUtil.getMethodInfo(ClassWithStuffa.class, "<init>", "(Lcom/newrelic/agent/profile/method/MethodInfoTest;)V");
        Assert.assertNotNull(constructor);
        Assert.assertTrue(constructor instanceof ExactMethodInfo);
        MethodInfo wrongParam = MethodInfoUtil.getMethodInfo(ClassWithStuffa.class, "<init>", "(LI;)V");
        Assert.assertNotNull(wrongParam);
        Assert.assertTrue(wrongParam instanceof MultipleMethodInfo);

        class ClassWithStuffb {

            public void foo() {

            }

            public void foo(String a) {

            }

            public void foo(String a, int b, long c) {

            }

            public void foo(byte a, int[] b, String[] c, List<String> d) {

            }
        }

        Assert.assertEquals(1, m.length);
        actual = MethodInfoUtil.getMethodInfo(ClassWithStuffb.class, "foo", "(Ljava.lang.String;)V");
        Assert.assertNotNull(actual);
        secondActual = actual.getJsonMethodMaps();
        Assert.assertNotNull(secondActual);
        Assert.assertEquals(1, secondActual.size());
        methodData = secondActual.get(0);
        Assert.assertNotNull(methodData);
        Assert.assertEquals(1, methodData.size());
        args = (List<String>) methodData.get("args");
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));

        Assert.assertEquals(1, m.length);
        actual = MethodInfoUtil.getMethodInfo(ClassWithStuffb.class, "foo", "(Ljava.lang.String;IJ)V");
        Assert.assertNotNull(actual);
        secondActual = actual.getJsonMethodMaps();
        Assert.assertNotNull(secondActual);
        Assert.assertEquals(1, secondActual.size());
        methodData = secondActual.get(0);
        Assert.assertNotNull(methodData);
        Assert.assertEquals(1, methodData.size());
        args = (List<String>) methodData.get("args");
        Assert.assertNotNull(args);
        Assert.assertEquals(3, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));
        Assert.assertEquals("int", args.get(1));
        Assert.assertEquals("long", args.get(2));

        Assert.assertEquals(1, m.length);
        actual = MethodInfoUtil.getMethodInfo(ClassWithStuffb.class, "foo", "(B[I[Ljava.lang.String;Ljava.util.List;)V");
        Assert.assertNotNull(actual);
        secondActual = actual.getJsonMethodMaps();
        Assert.assertNotNull(secondActual);
        Assert.assertEquals(1, secondActual.size());
        methodData = secondActual.get(0);
        Assert.assertNotNull(methodData);
        Assert.assertEquals(1, methodData.size());
        args = (List<String>) methodData.get("args");
        Assert.assertNotNull(args);
        Assert.assertEquals(4, args.size());
        Assert.assertEquals("byte", args.get(0));
        Assert.assertEquals("int[]", args.get(1));
        Assert.assertEquals("java.lang.String[]", args.get(2));
        Assert.assertEquals("java.util.List", args.get(3));

    }

    @Test
    public void testGetMethodWithDescription() {
        class ClassWithStuffa {
            private final long myField;

            public ClassWithStuffa() {
                myField = 0;
            }

            public long getMyField() {
                return myField;
            }
        }
        List<Member> output = new ArrayList<>();

        // jacoco will cause issues here
        Method[] m = filterJacocoMethods(ClassWithStuffa.class.getDeclaredMethods());
        Assert.assertEquals(1, m.length);
        boolean actual = MethodInfoUtil.getMethod(output, ClassWithStuffa.class, m[0].getName(),
                new ArrayList<String>());
        Assert.assertTrue(actual);
        Assert.assertEquals(1, output.size());
        Assert.assertEquals(m[0], output.get(0));

        class ClassWithStuffb {

            public void foo() {

            }

            public void foo(String a) {

            }

            public void foo(String a, int b, long c) {

            }

            public void foo(byte a, int[] b, String[] c, List<String> d) {

            }
        }

        output.clear();
        actual = MethodInfoUtil.getMethod(output, ClassWithStuffb.class, "foo", Arrays.asList("java.lang.String",
                "java.lang.String"));
        Assert.assertFalse(actual);
        // it can not find a method with two string args and so it returns both
        Assert.assertEquals(4, output.size());

        output.clear();
        actual = MethodInfoUtil.getMethod(output, ClassWithStuffb.class, "foo", Collections.singletonList("java.lang.String"));
        Assert.assertTrue(actual);
        Assert.assertEquals(1, output.size());
        Member member = output.get(0);
        Assert.assertEquals("foo", member.getName());
        Assert.assertEquals(1, MethodInfoUtil.getArguments(member).size());

        output.clear();
        actual = MethodInfoUtil.getMethod(output, ClassWithStuffb.class, "foo", Arrays.asList("java.lang.String",
                "int", "long"));
        Assert.assertTrue(actual);
        Assert.assertEquals(1, output.size());
        member = output.get(0);
        Assert.assertEquals("foo", member.getName());
        Assert.assertEquals(3, MethodInfoUtil.getArguments(member).size());

        output.clear();
        actual = MethodInfoUtil.getMethod(output, ClassWithStuffb.class, "foo", Arrays.asList("java.lang.String",
                "int", "long", "long"));
        // it can not find a method with two string args and so it returns both
        Assert.assertFalse(actual);
        Assert.assertEquals(4, output.size());

        output.clear();
        actual = MethodInfoUtil.getMethod(output, ClassWithStuffb.class, "foo", Arrays.asList("byte", "int[]",
                "java.lang.String[]", "java.util.List"));
        Assert.assertTrue(actual);
        Assert.assertEquals(1, output.size());
        member = output.get(0);
        Assert.assertEquals("foo", member.getName());
        Assert.assertEquals(4, MethodInfoUtil.getArguments(member).size());

    }

    @Test
    public void testGetMethod() {
        class ClassWithStuffa {
            private final long myField;

            public ClassWithStuffa() {
                myField = 0;
            }

            public long getMyField() {
                return myField;
            }
        }
        Method[] m = filterJacocoMethods(ClassWithStuffa.class.getDeclaredMethods());
        Assert.assertEquals(1, m.length);
        Set<Member> actual = MethodInfoUtil.getMatchingMethods(ClassWithStuffa.class, m[0].getName());
        Assert.assertEquals(1, actual.size());
        Member member = actual.iterator().next();
        Assert.assertEquals("getMyField", member.getName());

        actual = MethodInfoUtil.getMatchingMethods(ClassWithStuffa.class, "getMyField");
        Assert.assertEquals(1, actual.size());
        member = actual.iterator().next();
        Assert.assertEquals("getMyField", member.getName());

        m = ClassWithStuff1.class.getDeclaredMethods();
        actual = MethodInfoUtil.getMatchingMethods(ClassWithStuff1.class, "doWork");
        Assert.assertEquals(1, actual.size());
        member = actual.iterator().next();
        Assert.assertEquals("doWork", member.getName());

        class ClassWithStuffb {

            public void foo() {

            }

            public void foo(String a) {

            }

            public void foo(String a, int b, int c) {

            }
        }

        m = ClassWithStuffb.class.getDeclaredMethods();
        actual = MethodInfoUtil.getMatchingMethods(ClassWithStuffb.class, "notPresent");
        Assert.assertEquals(0, actual.size());

        m = ClassWithStuffb.class.getDeclaredMethods();
        actual = MethodInfoUtil.getMatchingMethods(ClassWithStuffb.class, "foo");
        Assert.assertEquals(3, actual.size());
        member = actual.iterator().next();
        Assert.assertEquals("foo", member.getName());
        member = actual.iterator().next();
        Assert.assertEquals("foo", member.getName());
        member = actual.iterator().next();
        Assert.assertEquals("foo", member.getName());
    }

    @Test
    public void testGetArgumentsConstructors() {
        class ClassWithStuffa {
            private final long myField;

            public ClassWithStuffa() {
                myField = 0;
            }
        }
        Constructor[] cons = ClassWithStuffa.class.getConstructors();
        Assert.assertEquals(1, cons.length);
        List<String> args = assertBasicsAndReturnArgsForGetArgs(cons[0]);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("com.newrelic.agent.profile.method.MethodInfoTest", args.get(0));

        cons = ClassWithStuff1.class.getConstructors();
        Assert.assertEquals(1, cons.length);
        args = assertBasicsAndReturnArgsForGetArgs(cons[0]);
        Assert.assertEquals(0, args.size());

        cons = ClassWithStuff2.class.getConstructors();
        Assert.assertEquals(1, cons.length);
        args = assertBasicsAndReturnArgsForGetArgs(cons[0]);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("long", args.get(0));

        cons = ClassWithStuff3.class.getConstructors();
        Assert.assertEquals(1, cons.length);
        args = assertBasicsAndReturnArgsForGetArgs(cons[0]);
        Assert.assertEquals(5, args.size());
        Assert.assertEquals("long", args.get(0));
        Assert.assertEquals("int", args.get(1));
        Assert.assertEquals("java.lang.String", args.get(2));
        Assert.assertEquals("java.lang.Object", args.get(3));
        Assert.assertEquals("java.util.List", args.get(4));
    }

    @Test
    public void testGetArgumentsMethod() {
        class ClassWithStuffa {
            private final long myField;

            public ClassWithStuffa() {
                myField = 0;
            }

            public long getMyField() {
                return myField;
            }
        }
        Method[] m = filterJacocoMethods(ClassWithStuffa.class.getDeclaredMethods());
        Assert.assertEquals(1, m.length);
        List<String> args = assertBasicsAndReturnArgsForGetArgs(m[0]);
        Assert.assertEquals(0, args.size());

        m = filterJacocoMethods(ClassWithStuff1.class.getDeclaredMethods());
        Assert.assertEquals(1, m.length);
        args = assertBasicsAndReturnArgsForGetArgs(m[0]);
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("int", args.get(0));

        m = filterJacocoMethods(ClassWithStuff2.class.getDeclaredMethods());
        Assert.assertEquals(1, m.length);
        args = assertBasicsAndReturnArgsForGetArgs(m[0]);
        Assert.assertEquals(4, args.size());
        Assert.assertEquals("int", args.get(0));
        Assert.assertEquals("short", args.get(1));
        Assert.assertEquals("int", args.get(2));
        Assert.assertEquals("long", args.get(3));

        m = filterJacocoMethods(ClassWithStuff3.class.getDeclaredMethods());
        Assert.assertEquals(1, m.length);
        args = assertBasicsAndReturnArgsForGetArgs(m[0]);
        Assert.assertEquals(5, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));
        Assert.assertEquals("java.util.List", args.get(1));
        Assert.assertEquals("java.lang.Integer", args.get(2));
        Assert.assertEquals("int[]", args.get(3));
        Assert.assertEquals("java.lang.Object", args.get(4));
    }

    @Test
    public void testGetArgumentsField() {
        class ClassWithStuffa {
            private final long myField;

            public ClassWithStuffa() {
                myField = 0;
            }

            public long getMyField() {
                return myField;
            }
        }
        Field[] m = ClassWithStuffa.class.getDeclaredFields();
        List<String> args = assertBasicsAndReturnArgsForGetArgs(m[0]);
        Assert.assertEquals(0, args.size());
    }

    private List<String> assertBasicsAndReturnArgsForGetArgs(Member current) {
        List<String> actual = MethodInfoUtil.getArguments(current);
        List<String> secondActual;
        if (current instanceof Method) {
            secondActual = MethodInfoUtil.getArguments(Type.getMethodDescriptor((Method) current));
        } else if (current instanceof Constructor) {
            secondActual = MethodInfoUtil.getArguments(Type.getConstructorDescriptor((Constructor) current));
        } else {
            secondActual = null;
        }

        Assert.assertNotNull(actual);
        if (secondActual != null) {
            Assert.assertEquals(secondActual.size(), actual.size());
            for (int i = 0; i < actual.size(); i++) {
                Assert.assertEquals(secondActual.get(i), actual.get(i));
            }
        }
        return actual;
    }

    public static class ClassWithStuff1 {
        private final long myField;

        public ClassWithStuff1() {
            myField = 0;
        }

        public long doWork(int val) {
            return myField;
        }

    }

    public static class ClassWithStuff2 {
        private final long myField;

        public ClassWithStuff2(long myfield) {
            myField = myfield;
        }

        public void bar(int a, short b, int c, long d) {

        }
    }

    public static class ClassWithStuff3 {
        private final long myField;

        public ClassWithStuff3(long a, int b, String c, Object d, List<String> e) {
            myField = b;
        }

        public void bar(String a, List<String> b, Integer d, int[] e, Object f) {

        }
    }



    // jacoco inserts itself into our tests causing ocnflicts with assertions.
    // we need to filter out these methods.
    private Method[] filterJacocoMethods(Method[] methods) {
        return Arrays.stream(methods)
                .filter(method -> !method.toString().contains("$jacoco"))
                .toArray(Method[]::new);
    }

}
