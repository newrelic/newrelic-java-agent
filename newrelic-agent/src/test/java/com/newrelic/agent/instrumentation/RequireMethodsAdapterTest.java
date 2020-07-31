/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Method;

public class RequireMethodsAdapterTest {

    @Test
    public void testSimpleClass() throws IOException {
        visitRequireMethodsAdapter(Test1.class, IOurTest1.class);
    }

    @Test
    public void testSimpleClass2() throws IOException {
        visitRequireMethodsAdapter2(Test1.class, IOurTest1.class);
    }

    @Test(expected = StopProcessingException.class)
    public void testBadReturnType() throws IOException {
        visitRequireMethodsAdapter(Test5.class, IOurTest1.class);
    }

    @Test(expected = StopProcessingException.class)
    public void testClassMissingMethods() throws IOException {
        visitRequireMethodsAdapter(Test2.class, IOurTest1234.class);
    }

    @Test
    public void testSuperClass() throws IOException {
        visitRequireMethodsAdapter(Test4.class, IOurTest1234.class);
    }

    @Test
    public void testSimpleInterface() throws IOException {
        visitRequireMethodsAdapter(ITest1.class, IOurTest1.class);
    }

    @Test
    public void testSuperInterface() throws IOException {
        visitRequireMethodsAdapter(ITest4.class, IOurTest1234.class);
    }

    @Test(expected = StopProcessingException.class)
    public void testInterfaceMissingMethods() throws IOException {
        visitRequireMethodsAdapter(ITest2.class, IOurTest1234.class);
    }

    private void visitRequireMethodsAdapter(Class<?> clazz, Class<?> type) throws IOException {
        ClassReader cr = getClassReader(clazz);
        RequireMethodsAdapter adapter = RequireMethodsAdapter.getRequireMethodsAdaptor(null, clazz.getName(), type,
                clazz.getClassLoader());
        cr.accept(adapter, 0);
    }

    private void visitRequireMethodsAdapter2(Class<?> clazz, Class<?> type) throws IOException {
        ClassReader cr = getClassReader(clazz);
        Set<Method> methods = InstrumentationUtils.getDeclaredMethods(type);
        RequireMethodsAdapter adapter = RequireMethodsAdapter.getRequireMethodsAdaptor(null, methods, type.getName(),
                clazz.getName(), clazz.getClassLoader());
        cr.accept(adapter, 0);
    }

    private ClassReader getClassReader(Class<?> clazz) throws IOException {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            return new ClassReader(is);
        }
    }

}
