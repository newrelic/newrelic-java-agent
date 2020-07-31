/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MethodMappersAdapterTest {

    @Test
    public void test() throws IOException {
        ClassReader cr = visitMethodMappersAdaptor(Test1.class, InterfaceMapperTest1.class);
        String[] result = cr.getInterfaces();
        Assert.assertEquals(0, result.length);
        GetMethodsVisitor classVisitor = new GetMethodsVisitor();
        cr.accept(classVisitor, 0);
        Map<String, Method> allMethods = classVisitor.getAllMethods();
        Method method = allMethods.get("test1");
        Assert.assertNotNull(method);
        Assert.assertEquals("test1", method.getName());
        Assert.assertEquals("()Ljava/lang/String;", method.getDescriptor());
        method = allMethods.get("_nr_test1");
        Assert.assertNotNull(method);
        Assert.assertEquals("_nr_test1", method.getName());
        Assert.assertEquals("()Ljava/lang/Object;", method.getDescriptor());
    }

    @Test
    public void testSuperclassMappedMethod() throws IOException {
        ClassReader cr = visitMethodMappersAdaptor(Test2.class, InterfaceMapperTest2.class);
        String[] result = cr.getInterfaces();
        Assert.assertEquals(0, result.length);
        GetMethodsVisitor classVisitor = new GetMethodsVisitor();
        cr.accept(classVisitor, 0);
        Map<String, Method> allMethods = classVisitor.getAllMethods();
        Method method = allMethods.get("test1");
        Assert.assertNull(method);
        method = allMethods.get("_nr_test1");
        Assert.assertNull(method);
    }

    private ClassReader visitMethodMappersAdaptor(Class<?> clazz, Class<?> type) throws IOException {
        ClassReader cr = getClassReader(clazz);
        ClassWriter cw = new ClassWriter(cr, 0);
        MethodMappersAdapter adapter = MethodMappersAdapter.getMethodMappersAdapter(cw, type, clazz.getName());
        cr.accept(adapter, 0);
        byte[] classBytes = cw.toByteArray();
        return new ClassReader(classBytes);
    }

    private ClassReader getClassReader(Class<?> clazz) throws IOException {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            return new ClassReader(is);
        }
    }

    public static class GetMethodsVisitor extends ClassVisitor {

        private final Map<String, Method> allMethods = new HashMap<>();

        public GetMethodsVisitor() {
            super(WeaveUtils.ASM_API_LEVEL);
        }

        public Map<String, Method> getAllMethods() {
            return allMethods;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            Method method = new Method(name, desc);
            allMethods.put(name, method);
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

}
