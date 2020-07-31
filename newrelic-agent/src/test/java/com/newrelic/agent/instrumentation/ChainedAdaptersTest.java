/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.instrumentation.FieldAccessorGeneratingClassAdapterTest.GetFieldsVisitor;
import com.newrelic.agent.instrumentation.MethodMappersAdapterTest.GetMethodsVisitor;

public class ChainedAdaptersTest {

    @Test
    public void testFieldAccessors() throws IOException {
        ClassReader cr = visitMixinAdaptors(Test1.class, InterfaceMixinTest1.class);
        String[] result = cr.getInterfaces();
        String expected = InterfaceMixinTest1.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
        GetFieldsVisitor classVisitor = new GetFieldsVisitor();
        cr.accept(classVisitor, 0);
        Map<String, String> allFields = classVisitor.getAllFields();
        String desc = allFields.get("field1");
        Assert.assertNotNull(desc);
        Assert.assertEquals("Z", desc);
        desc = allFields.get("field2");
        Assert.assertNotNull(desc);
        Assert.assertEquals("Ljava/lang/String;", desc);
        desc = allFields.get("__nr__field3");
        Assert.assertNotNull(desc);
        Assert.assertEquals("Ljava/lang/Object;", desc);
    }

    @Test
    public void testMethodMappers() throws IOException {
        ClassReader cr = visitMethodMapperAdaptors(Test1.class, InterfaceMapperTest1.class);
        String[] result = cr.getInterfaces();
        String expected = InterfaceMapperTest1.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
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

    @Test(expected = StopProcessingException.class)
    public void testMethodMappersMissingMethods() throws IOException {
        visitMethodMapperAdaptors(Test1.class, InterfaceMapperMissingMethodTest1.class);
    }

    @Test
    public void testMultipleMixins() throws IOException {
        ClassReader cr = visitMixinAdaptors(Test4.class, InterfaceMixinTest2.class, InterfaceMixinTest3.class);
        String[] result = cr.getInterfaces();
        String expected = InterfaceMixinTest2.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
        expected = InterfaceMixinTest3.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testSimpleClass() throws IOException {
        ClassReader cr = visitMixinAdaptors(Test1.class, IOurTest1.class);
        String[] result = cr.getInterfaces();
        String expected = IOurTest1.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testSuperClass() throws IOException {
        ClassReader cr = visitMixinAdaptors(Test4.class, IOurTest1234.class);
        String[] result = cr.getInterfaces();
        String expected = IOurTest1234.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testSimpleInterface() throws IOException {
        ClassReader cr = visitMixinAdaptors(ITest1.class, IOurTest1.class);
        String[] result = cr.getInterfaces();
        String expected = IOurTest1.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testSuperInterface() throws IOException {
        ClassReader cr = visitMixinAdaptors(ITest4.class, IOurTest1234.class);
        String[] result = cr.getInterfaces();
        String expected = IOurTest1234.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test(expected = StopProcessingException.class)
    public void testClassMissingMethods() throws IOException {
        visitMixinAdaptors(Test2.class, IOurTest1234.class);
    }

    @Test(expected = StopProcessingException.class)
    public void testInterfaceMissingMethods() throws IOException {
        visitMixinAdaptors(ITest2.class, IOurTest1234.class);
    }

    private ClassReader visitMixinAdaptors(Class<?> clazz, Class<?>... types) throws IOException {
        ClassReader cr = getClassReader(clazz);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor classVisitor = cw;
        for (Class<?> type : types) {
            classVisitor = getMixinAdaptor(classVisitor, clazz, type);
        }
        cr.accept(classVisitor, 0);
        byte[] classBytes = cw.toByteArray();
        return new ClassReader(classBytes);
    }

    private ClassVisitor getMixinAdaptor(ClassVisitor classVisitor, Class<?> clazz, Class<?> type) {
        ClassVisitor adapter = new AddInterfaceAdapter(classVisitor, clazz.getName(), type);
        adapter = RequireMethodsAdapter.getRequireMethodsAdaptor(adapter, clazz.getName(), type, clazz.getClassLoader());
        adapter = new FieldAccessorGeneratingClassAdapter(adapter, clazz.getName(), type);
        return adapter;
    }

    private ClassReader visitMethodMapperAdaptors(Class<?> clazz, Class<?> type) throws IOException {
        ClassReader cr = getClassReader(clazz);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor adapter = getMethodMapperAdaptor(cw, clazz, type);
        cr.accept(adapter, 0);
        byte[] classBytes = cw.toByteArray();
        return new ClassReader(classBytes);
    }

    private ClassVisitor getMethodMapperAdaptor(ClassVisitor classVisitor, Class<?> clazz, Class<?> type) {
        ClassVisitor adapter = new AddInterfaceAdapter(classVisitor, clazz.getName(), type);
        adapter = RequireMethodsAdapter.getRequireMethodsAdaptor(adapter, clazz.getName(), type, clazz.getClassLoader());
        adapter = MethodMappersAdapter.getMethodMappersAdapter(adapter, type, clazz.getName());
        return adapter;
    }

    private ClassReader getClassReader(Class<?> clazz) throws IOException {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            return new ClassReader(is);
        }
    }

}
