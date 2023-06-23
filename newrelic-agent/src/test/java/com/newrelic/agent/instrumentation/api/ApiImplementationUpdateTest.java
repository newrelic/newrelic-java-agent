/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.api;

import com.newrelic.agent.instrumentation.RequireMethodsAdapter;
import com.newrelic.agent.instrumentation.StopProcessingException;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcherTest;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class ApiImplementationUpdateTest {

    private static Set<Method> REQUEST_METHODS;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DefaultApiImplementations defaultApiImplementations = new DefaultApiImplementations();
        REQUEST_METHODS = defaultApiImplementations.getApiClassNameToDefaultMethods().get(
                Request.class.getName().replace('.', '/')).keySet();
    }

    @Test
    public void testMatcher() throws Exception {
        InstrumentationContext iInstrumentationContext = Mockito.mock(InstrumentationContext.class);

        Class<?> clazz = RequestImpl.class;
        Set<Method> methodsToRemove = new HashSet<>();
        byte[] classBytes = removeMethods(clazz, methodsToRemove);

        // verify no missing methods
        RequireMethodsAdapter adapter = getRequireMethodsAdapter(clazz, Request.class, REQUEST_METHODS);
        expectNoMissingMethods(adapter, classBytes);

        ApiImplementationUpdate transformer = new ApiImplementationUpdate();
        ClassMatchVisitorFactory matcher = transformer.getMatcher();
        ClassReader reader = new ClassReader(new ByteArrayInputStream(classBytes));
        ClassVisitor visitor = matcher.newClassMatchVisitor(clazz.getClassLoader(), null, reader, null,
                iInstrumentationContext);
        reader.accept(visitor, ClassReader.SKIP_CODE);

        Mockito.verify(iInstrumentationContext, Mockito.never()).putMatch(
                Mockito.<ClassMatchVisitorFactory> any(), Mockito.<Match> any());

    }

    @Test
    public void testMatcherMissingMethods() throws Exception {
        ArgumentCaptor<ClassMatchVisitorFactory> arg = ArgumentCaptor.forClass(ClassMatchVisitorFactory.class);
        InstrumentationContext iInstrumentationContext = Mockito.mock(InstrumentationContext.class);

        // remove required methods from Request implementation class
        Class<?> clazz = RequestImpl.class;
        Set<Method> methodsToRemove = new HashSet<>();
        methodsToRemove.add(new Method("getHeaderType", "()Lcom/newrelic/api/agent/HeaderType;"));
        byte[] classBytes = removeMethods(clazz, methodsToRemove);

        // verify missing methods
        RequireMethodsAdapter adapter = getRequireMethodsAdapter(clazz, Request.class, REQUEST_METHODS);
        expectMissingMethods(adapter, classBytes);

        ApiImplementationUpdate transformer = new ApiImplementationUpdate();
        ClassMatchVisitorFactory matcher = transformer.getMatcher();
        ClassReader reader = new ClassReader(new ByteArrayInputStream(classBytes));
        ClassVisitor visitor = matcher.newClassMatchVisitor(clazz.getClassLoader(), null, reader, null,
                iInstrumentationContext);
        reader.accept(visitor, ClassReader.SKIP_CODE);

        Mockito.verify(iInstrumentationContext, Mockito.only()).putMatch(arg.capture(), Mockito.<Match> any());
        Assert.assertSame(matcher, arg.getValue());

    }

    @Test
    public void testTransform() throws Exception {
        // remove required methods from Request implementation class
        Class<?> clazz = RequestImpl.class;
        Set<Method> methodsToRemove = new HashSet<>();
        methodsToRemove.add(new Method("getHeaderType", "()Lcom/newrelic/api/agent/HeaderType;"));
        byte[] classBytes = removeMethods(clazz, methodsToRemove);

        // verify missing methods
        RequireMethodsAdapter adapter = getRequireMethodsAdapter(clazz, Request.class, REQUEST_METHODS);
        expectMissingMethods(adapter, classBytes);

        // should add default implementations for missing methods
        ApiImplementationUpdate transformer = new ApiImplementationUpdate();
        classBytes = transformer.transform(clazz.getClassLoader(), null, null, null, classBytes, null, null);

        // verify no missing methods
        adapter = getRequireMethodsAdapter(clazz, Request.class, REQUEST_METHODS);
        expectNoMissingMethods(adapter, classBytes);
    }

    private byte[] removeMethods(Class<?> clazz, Set<Method> methodsToRemove) throws IOException {
        ClassReader reader = ClassMatcherTest.getClassReader(clazz);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new RemoveMethodsClassVisitor(writer, methodsToRemove);
        reader.accept(cv, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private void expectMissingMethods(RequireMethodsAdapter adapter, byte[] classBytes) throws IOException {
        ClassReader reader = new ClassReader(new ByteArrayInputStream(classBytes));
        try {
            reader.accept(adapter, 0);
        } catch (StopProcessingException ex) {
            // expected
        }
    }

    private void expectNoMissingMethods(RequireMethodsAdapter adapter, byte[] classBytes) throws IOException {
        ClassReader reader = new ClassReader(new ByteArrayInputStream(classBytes));
        reader.accept(adapter, 0);
    }

    private RequireMethodsAdapter getRequireMethodsAdapter(Class<?> clazz, Class<?> type, Set<Method> requiredMethods)
            throws IOException {
        return RequireMethodsAdapter.getRequireMethodsAdaptor(null, requiredMethods, clazz.getName(), type.getName(),
                clazz.getClassLoader());
    }

    private static class RequestImpl implements Request {

        private RequestImpl() {
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public Enumeration<?> getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public HeaderType getHeaderType() {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }
    }
}
