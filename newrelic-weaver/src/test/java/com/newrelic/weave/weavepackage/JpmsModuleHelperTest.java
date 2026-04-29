/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class JpmsModuleHelperTest {

    // Skip the test if running on Java 8, where JPMS is unavailable.
    private static void requireJpms() {
        Assume.assumeFalse("Requires Java 9+ (JPMS)", "1.8".equals(System.getProperty("java.specification.version")));
    }

    @After
    public void resetMulePatched() throws Exception {
        Field f = JpmsModuleHelper.class.getDeclaredField("mulePatched");
        f.setAccessible(true);
        ((AtomicBoolean) f.get(null)).set(false);
    }

    @Test
    public void noOpWhenInstrumentationIsNull() {
        // Must not throw regardless of className/classloader
        JpmsModuleHelper.addReadsToUnnamedModule(null, "org/mule/runtime/Core", getClass().getClassLoader());
    }

    @Test
    public void noOpWhenClassNameIsNull() {
        Instrumentation inst = mock(Instrumentation.class);
        JpmsModuleHelper.addReadsToUnnamedModule(inst, null, getClass().getClassLoader());
        verifyNoInteractions(inst);
    }

    @Test
    public void noOpWhenClassLoaderIsNull() {
        Instrumentation inst = mock(Instrumentation.class);
        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/Core", null);
        verifyNoInteractions(inst);
    }

    @Test
    public void noOpForNonMuleClass() {
        Instrumentation inst = mock(Instrumentation.class);
        JpmsModuleHelper.addReadsToUnnamedModule(inst, "com/example/SomeClass", getClass().getClassLoader());
        verifyNoInteractions(inst);
    }

    @Test
    public void noOpForMule3PackagePrefix() {
        // org.mule.* (Mule 3.x) must NOT trigger the patch, only org.mule.runtime.*
        Instrumentation inst = mock(Instrumentation.class);
        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/api/processor/MessageProcessor", getClass().getClassLoader());
        verifyNoInteractions(inst);
    }

    @Test
    public void muleClassTriggersScan() {
        requireJpms();
        Instrumentation inst = mock(Instrumentation.class);
        when(inst.getAllLoadedClasses()).thenReturn(new Class<?>[0]);

        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/core/Test", getClass().getClassLoader());

        verify(inst, times(1)).getAllLoadedClasses();
    }

    @Test
    public void bulkPatchRunsOnlyOnce() {
        requireJpms();
        Instrumentation inst = mock(Instrumentation.class);
        when(inst.getAllLoadedClasses()).thenReturn(new Class<?>[0]);
        ClassLoader cl = getClass().getClassLoader();

        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/core/Test", cl);
        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/api/Test2", cl);
        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/core/Test3", cl);

        verify(inst, times(1)).getAllLoadedClasses();
    }

    @Test
    public void concurrentMuleCallsOnlyPatchOnce() throws Exception {
        requireJpms();
        Instrumentation inst = mock(Instrumentation.class);
        when(inst.getAllLoadedClasses()).thenReturn(new Class<?>[0]);
        ClassLoader cl = getClass().getClassLoader();

        int threadCount = 20;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                ready.countDown();
                try { ready.await(); } catch (InterruptedException ignored) {}
                JpmsModuleHelper.addReadsToUnnamedModule(inst,
                        "org/mule/runtime/core/Thread" + idx, cl);
                done.countDown();
            }).start();
        }

        done.await();
        verify(inst, times(1)).getAllLoadedClasses();
    }

    @Test
    public void unwrapTraversesDelegateChain() {
        requireJpms();
        Instrumentation real = mock(Instrumentation.class);
        when(real.getAllLoadedClasses()).thenReturn(new Class<?>[0]);

        DelegatingInstrumentation proxy = new DelegatingInstrumentation(real);

        JpmsModuleHelper.addReadsToUnnamedModule(proxy, "org/mule/runtime/core/Test", getClass().getClassLoader());

        verify(real, times(1)).getAllLoadedClasses();
    }

    @Test
    public void unwrapHandlesMultipleDelegateLevels() {
        requireJpms();
        Instrumentation real = mock(Instrumentation.class);
        when(real.getAllLoadedClasses()).thenReturn(new Class<?>[0]);

        DelegatingInstrumentation proxy1 = new DelegatingInstrumentation(real);
        DelegatingInstrumentation proxy2 = new DelegatingInstrumentation(proxy1);

        JpmsModuleHelper.addReadsToUnnamedModule(proxy2, "org/mule/runtime/core/Test", getClass().getClassLoader());

        verify(real, times(1)).getAllLoadedClasses();
    }

    @Test
    public void scanLoopSkipsUnnamedModuleClasses() {
        requireJpms();
        Instrumentation inst = mock(Instrumentation.class);
        when(inst.getAllLoadedClasses()).thenReturn(new Class<?>[]{JpmsModuleHelperTest.class});

        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/core/Test", getClass().getClassLoader());

        verify(inst, times(1)).getAllLoadedClasses();
    }

    @Test
    public void scanLoopAddsNamedModulesToReadsSet() {
        requireJpms();
        Instrumentation inst = mock(Instrumentation.class);
        when(inst.getAllLoadedClasses()).thenReturn(new Class<?>[]{String.class});

        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/core/Test", getClass().getClassLoader());

        verify(inst, times(1)).getAllLoadedClasses();
    }

    @Test
    public void mulePatched_falseBeforeFirstCall() throws Exception {
        Field f = JpmsModuleHelper.class.getDeclaredField("mulePatched");
        f.setAccessible(true);
        assertFalse(((AtomicBoolean) f.get(null)).get());
    }

    @Test
    public void mulePatched_trueAfterFirstMuleCall() throws Exception {
        requireJpms();
        Instrumentation inst = mock(Instrumentation.class);
        when(inst.getAllLoadedClasses()).thenReturn(new Class<?>[0]);

        JpmsModuleHelper.addReadsToUnnamedModule(inst, "org/mule/runtime/core/Test", getClass().getClassLoader());

        Field f = JpmsModuleHelper.class.getDeclaredField("mulePatched");
        f.setAccessible(true);
        assertTrue(((AtomicBoolean) f.get(null)).get());
    }

    @Test
    public void mulePatched_remainsFalseForNonMuleClass() throws Exception {
        Instrumentation inst = mock(Instrumentation.class);
        JpmsModuleHelper.addReadsToUnnamedModule(inst, "com/example/Test", getClass().getClassLoader());

        Field f = JpmsModuleHelper.class.getDeclaredField("mulePatched");
        f.setAccessible(true);
        assertFalse(((AtomicBoolean) f.get(null)).get());
    }

    private static class DelegatingInstrumentation implements Instrumentation {
        @SuppressWarnings("unused")
        private final Instrumentation delegate;

        DelegatingInstrumentation(Instrumentation delegate) {
            this.delegate = delegate;
        }

        @Override public void addTransformer(ClassFileTransformer t, boolean b) {}
        @Override public void addTransformer(ClassFileTransformer t) {}
        @Override public boolean removeTransformer(ClassFileTransformer t) { return false; }
        @Override public boolean isRetransformClassesSupported() { return false; }
        @Override public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {}
        @Override public boolean isRedefineClassesSupported() { return false; }
        @Override public void redefineClasses(ClassDefinition... d) throws ClassNotFoundException, UnmodifiableClassException {}
        @Override public boolean isModifiableClass(Class<?> c) { return false; }
        @Override public Class<?>[] getAllLoadedClasses() { return null; }
        @Override public Class<?>[] getInitiatedClasses(ClassLoader l) { return new Class<?>[0]; }
        @Override public long getObjectSize(Object o) { return 0; }
        @Override public void appendToBootstrapClassLoaderSearch(JarFile j) {}
        @Override public void appendToSystemClassLoaderSearch(JarFile j) {}
        @Override public boolean isNativeMethodPrefixSupported() { return false; }
        @Override public void setNativeMethodPrefix(ClassFileTransformer t, String p) {}
    }
}
