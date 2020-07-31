/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import com.newrelic.agent.instrumentation.FieldAccessorGeneratingClassAdapterTest.GetFieldsVisitor;
import com.newrelic.weave.utils.Streams;

public class InterfaceImplementationClassTransformerTest {

    @Before
    public void beforeTest() throws Exception {
        AgentTestUtils.createServiceManager(new HashMap<String, Object>());
    }

    @Test
    public void testMixin() throws Exception {
        InterfaceMixinClassTransformer classTransformer = new InterfaceMixinClassTransformer(0);
        classTransformer.addInterfaceMixin(InterfaceMixinTest1.class);
        byte[] classBytes = getClassBytes(Test1.class);
        byte[] transformedBytes = classTransformer.transform(Test1.class.getClassLoader(),
                Test1.class.getName().replace('.', '/'), null, null, classBytes);
        ClassReader cr = new ClassReader(transformedBytes);
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
    public void testMultipleMixins() throws Exception {
        InterfaceMixinClassTransformer classTransformer = new InterfaceMixinClassTransformer(0);
        classTransformer.addInterfaceMixin(InterfaceMixinTest2.class);
        classTransformer.addInterfaceMixin(InterfaceMixinTest3.class);
        byte[] classBytes = getClassBytes(Test4.class);
        byte[] transformedBytes = classTransformer.transform(Test4.class.getClassLoader(),
                Test4.class.getName().replace('.', '/'), null, null, classBytes);
        ClassReader cr = new ClassReader(transformedBytes);
        String[] result = cr.getInterfaces();
        String expected = InterfaceMixinTest2.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
        expected = InterfaceMixinTest3.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testMultipleMixinsBad() throws Exception {
        InterfaceMixinClassTransformer classTransformer = new InterfaceMixinClassTransformer(0);
        classTransformer.addInterfaceMixin(InterfaceMixinTest2.class);
        classTransformer.addInterfaceMixin(InterfaceMixinMissingMethodTest4.class);
        byte[] classBytes = getClassBytes(Test4.class);
        byte[] transformedBytes = classTransformer.transform(Test4.class.getClassLoader(),
                Test4.class.getName().replace('.', '/'), null, null, classBytes);
        ClassReader cr = new ClassReader(transformedBytes);
        String[] result = cr.getInterfaces();
        String expected = InterfaceMixinTest2.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testMultipleMixinsBadReversed() throws Exception {
        InterfaceMixinClassTransformer classTransformer = new InterfaceMixinClassTransformer(0);
        classTransformer.addInterfaceMixin(InterfaceMixinMissingMethodTest4.class);
        classTransformer.addInterfaceMixin(InterfaceMixinTest2.class);
        byte[] classBytes = getClassBytes(Test4.class);
        byte[] transformedBytes = classTransformer.transform(Test4.class.getClassLoader(),
                Test4.class.getName().replace('.', '/'), null, null, classBytes);
        ClassReader cr = new ClassReader(transformedBytes);
        String[] result = cr.getInterfaces();
        String expected = InterfaceMixinTest2.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    private byte[] getClassBytes(Class<?> clazz) throws IOException {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Streams.copy(is, os);
            return os.toByteArray();
        }
    }

}
