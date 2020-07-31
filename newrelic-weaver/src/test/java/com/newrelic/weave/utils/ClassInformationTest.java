/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

/**
 * ClassInformationTest.java
 */
public class ClassInformationTest {
    private ClassInformationFinder finder;

    @Before
    public void before() {
        finder = new ClassCache(new ClassLoaderFinder(Thread.currentThread().getContextClassLoader()));
    }

    @Test
    public void testRegularClass() throws IOException {
        ClassInformation classInfo = finder.getClassInformation(ClassInfoTest.class.getCanonicalName() + "$RegularClass");
        Assert.assertNotNull(classInfo);
        Assert.assertEquals(0, classInfo.getAllInterfaces(finder).size());
        Assert.assertTrue(classInfo.getAllSuperNames(finder).contains(
                Object.class.getCanonicalName().replace('.', '/')));
        Assert.assertEquals(1, classInfo.getAllSuperNames(finder).size());

        Assert.assertTrue(classInfo.getAllMethods(finder).contains(
                new ClassInformation.MemberInformation("instanceMethod", "(Ljava/lang/Object;)J", Opcodes.ACC_PUBLIC)));
        Assert.assertTrue(classInfo.getAllMethods(finder).contains(
                new ClassInformation.MemberInformation("staticMethod", "(J)I",
                        (Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC))));
        Assert.assertTrue(classInfo.getAllMethods(finder).contains(
                new ClassInformation.MemberInformation("staticMethod", "(Ljava/lang/String;)I",
                        (Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC))));
    }

    /**
     * Trying to find info for a missing class should not throw an exception.
     */
    @Test
    public void testMissingClass() throws IOException {
        ClassInformation objectInfo = finder.getClassInformation("java.lang.Object");
        Assert.assertNotNull(objectInfo);
        objectInfo.superName = "foo/bar/Baz"; // class finder won't be able to resolve this.
        Assert.assertEquals(1, objectInfo.getAllSuperNames(finder).size());
        objectInfo.interfaceNames.add("not/a/Class"); // won't find this either
        Assert.assertEquals(1, objectInfo.getAllInterfaces(finder).size());
    }

    @Test
    public void testComplicatedHierarchy() throws IOException {
        ClassInformation classInfo = finder.getClassInformation(ClassInfoTest.class.getCanonicalName() + "$ImplClass");
        Assert.assertNotNull(classInfo);
        List<String> supers = new ArrayList<>(classInfo.getAllSuperNames(finder));
        List<String> interfaces = new ArrayList<>(classInfo.getAllInterfaces(finder));

        Assert.assertTrue(supers.contains(Object.class.getCanonicalName().replace('.', '/')));
        Assert.assertTrue(supers.contains(ClassInfoTest.class.getCanonicalName().replace('.', '/') + "$SuperClass1"));
        Assert.assertTrue(supers.contains(ClassInfoTest.class.getCanonicalName().replace('.', '/') + "$SuperClass2"));
        Assert.assertEquals(3, supers.size());

        Assert.assertTrue(interfaces.contains(ClassInfoTest.class.getCanonicalName().replace('.', '/') + "$Interface1"));
        Assert.assertTrue(interfaces.contains(ClassInfoTest.class.getCanonicalName().replace('.', '/') + "$Interface2"));
        Assert.assertTrue(interfaces.contains(ClassInfoTest.class.getCanonicalName().replace('.', '/') + "$Interface3"));
        Assert.assertTrue(interfaces.contains(ClassInfoTest.class.getCanonicalName().replace('.', '/') + "$Interface4"));
        Assert.assertEquals(4, interfaces.size());

        Assert.assertTrue(classInfo.getAllMethods(finder).contains(
                new ClassInformation.MemberInformation("implMethod", "()V", Opcodes.ACC_PUBLIC)));
        Assert.assertTrue(classInfo.getAllMethods(finder).contains(
                new ClassInformation.MemberInformation("superMethod2", "()V", Opcodes.ACC_PUBLIC)));
        Assert.assertTrue(classInfo.getAllMethods(finder).contains(
                new ClassInformation.MemberInformation("superMethod1", "()V", Opcodes.ACC_PUBLIC)));

        Assert.assertTrue(classInfo.getAllFields(finder).contains(
                new ClassInformation.MemberInformation("superField1", "Ljava/lang/String;", Opcodes.ACC_PUBLIC)));
        Assert.assertTrue(classInfo.getAllFields(finder).contains(
                new ClassInformation.MemberInformation("superField2", "Ljava/lang/String;", Opcodes.ACC_PUBLIC)));
        Assert.assertTrue(classInfo.getAllFields(finder).contains(
                new ClassInformation.MemberInformation("implField", "Ljava/lang/String;", Opcodes.ACC_PUBLIC)));
    }

    public static class RegularClass {
        private String aprivate = "private";
        public String apublic = "public";

        public static int staticMethod(long l) {
            return 0;
        }

        public static int staticMethod(String s) {
            return staticMethod(Long.valueOf(s));
        }

        public long instanceMethod(Object o) {
            return 5l;
        }
    }

    public interface Interface1 {
        void foo();
    }

    public interface Interface2 extends Interface1 {
        void bar();
    }

    public interface Interface3 {
        void baz();
    }

    public interface Interface4 {
        void biz();
    }

    public static class SuperClass1 {
        public String superField1;

        public void superMethod1() {
        }

    }

    public abstract static class SuperClass2 extends SuperClass1 implements Interface4 {
        public String superField2;

        public void superMethod2() {
        }
    }

    public static class ImplClass extends SuperClass2 implements Interface3, Interface2, Interface4 {
        public String implField;

        public void implMethod() {
        }

        @Override
        public void foo() {
        }

        @Override
        public void bar() {
        }

        @Override
        public void baz() {
        }

        @Override
        public void biz() {
        }
    }
}
