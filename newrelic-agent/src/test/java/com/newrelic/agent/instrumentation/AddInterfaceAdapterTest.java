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

import org.junit.Assert;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class AddInterfaceAdapterTest {

    @Test
    public void testSimpleClass() throws IOException {
        String[] result = getInterfaces(Test1.class, IOurTest1.class);
        String expected = IOurTest1.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testSuperClass() throws IOException {
        String[] result = getInterfaces(Test4.class, IOurTest1234.class);
        String expected = IOurTest1234.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testSimpleInterface() throws IOException {
        String[] result = getInterfaces(ITest1.class, IOurTest1.class);
        String expected = IOurTest1.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    @Test
    public void testSuperInterface() throws IOException {
        String[] result = getInterfaces(ITest4.class, IOurTest1234.class);
        String expected = IOurTest1234.class.getName().replace('.', '/');
        Assert.assertTrue(Arrays.asList(result).contains(expected));
    }

    private String[] getInterfaces(Class<?> clazz, Class<?> type) throws IOException {
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
        AddInterfaceAdapter adapter = new AddInterfaceAdapter(cw, className, type);
        cr.accept(adapter, 0);
        byte[] classBytes = cw.toByteArray();
        cr = new ClassReader(classBytes);
        return cr.getInterfaces();
    }

}
