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
import org.objectweb.asm.FieldVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FieldAccessorGeneratingClassAdapterTest {

    @Test
    public void testFieldAccessors() throws IOException {
        ClassReader cr = getClassReader(Test1.class, InterfaceMixinTest1.class);
        String[] result = cr.getInterfaces();
        Assert.assertEquals(0, result.length);
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
    public void testNoFieldAccessors() throws IOException {
        ClassReader cr = getClassReader(Test4.class, IOurTest4.class);
        String[] result = cr.getInterfaces();
        Assert.assertEquals(0, result.length);
        GetFieldsVisitor classVisitor = new GetFieldsVisitor();
        cr.accept(classVisitor, 0);
        Map<String, String> allFields = classVisitor.getAllFields();
        Assert.assertEquals(0, allFields.size());
    }

    private ClassReader getClassReader(Class<?> clazz, Class<?> type) throws IOException {
        String className = clazz.getName().replace('.', '/') + ".class";
        InputStream is = null;
        ClassReader cr = null;
        try {
            is = clazz.getClassLoader().getResourceAsStream(className);
            cr = new ClassReader(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor adapter = new FieldAccessorGeneratingClassAdapter(cw, className, type);
        cr.accept(adapter, 0);
        byte[] classBytes = cw.toByteArray();
        return new ClassReader(classBytes);
    }

    public static class GetFieldsVisitor extends ClassVisitor {

        private final Map<String, String> allFields = new HashMap<>();

        public GetFieldsVisitor() {
            super(WeaveUtils.ASM_API_LEVEL);
        }

        public Map<String, String> getAllFields() {
            return allFields;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            allFields.put(name, desc);
            return super.visitField(access, name, desc, signature, value);
        }
    }

}
