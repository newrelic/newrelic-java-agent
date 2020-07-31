/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.classloading;

import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.NewClassAppender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentClassLoaderTest {
    
    private ClassLoader classLoader;

    @Before
    public void setup() throws Exception {
        classLoader = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
            // use custom classloader for test
        };
    }

    /**
     * Test multiple defineClass calls for the same class
     */
    @Test
    public void testConcurrentSingleDefineClass() throws Exception {
        int numThreads = 10;
        List<Future> futures = new ArrayList<>(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        final String concurrentClassName1 = "a.TestClazz";
        final byte[] concurrentClassBytes1 = ClassLoaderUtils.generateEmptyClass(WeaveUtils.getClassInternalName(concurrentClassName1));
        for (int i = 0; i < numThreads; ++i) {
            Runnable r = new Runnable() {
                @Override
                public void run() {                    
                    try {
                        for (int i = 0; i < 100; ++i) {
                            Class<?> definedClass = defineClass(concurrentClassName1, concurrentClassBytes1);
                            Assert.assertNotNull(definedClass);
                        }
                    } catch (InvocationTargetException e) {
                        // LinkageError is thrown when the same class is defined multiple times, so this ok
                        if (!(e.getCause() instanceof LinkageError)) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail();
                    }
                }
            };
            futures.add(executor.submit(r));
        }
        for (Future future : futures) {
            future.get();
        }
        Class<?> clazz = classLoader.loadClass(concurrentClassName1);
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(clazz.newInstance());
    }

    /**
     * Test multiple defineClass calls for different classes
     */
    @Test
    public void testConcurrentMultipleDefineClass() throws Exception {
        int numThreads = 10;
        List<Future> futures = new ArrayList<>(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        final String concurrentClassName1 = "a.TestClass1";
        final byte[] concurrentClassBytes1 = ClassLoaderUtils.generateEmptyClass(WeaveUtils.getClassInternalName(concurrentClassName1));
        final String concurrentClassName2 = "a.TestClass2";
        final byte[] concurrentClassBytes2 = ClassLoaderUtils.generateEmptyClass(WeaveUtils.getClassInternalName(concurrentClassName2));

        for (int i = 0; i < numThreads; ++i) {
            final int threadId = i;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 100; ++i) {
                            Class<?> definedClass;
                            if (threadId % 2 == 1) {
                                definedClass = defineClass(concurrentClassName1, concurrentClassBytes1);
                            } else {
                                definedClass = defineClass(concurrentClassName2, concurrentClassBytes2);
                            }
                            Assert.assertNotNull(definedClass);
                        }
                    } catch (InvocationTargetException e) {
                        // LinkageError is thrown when the same class is defined multiple times, so this ok
                        if (!(e.getCause() instanceof LinkageError)) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail();
                    }
                }
            };
            futures.add(executor.submit(r));
        }
        for (Future future : futures) {
            future.get();
        }
        Class<?> clazz1 = classLoader.loadClass(concurrentClassName1);
        Assert.assertNotNull(clazz1.newInstance());
        Class<?> clazz2 = classLoader.loadClass(concurrentClassName2);
        Assert.assertNotNull(clazz2.newInstance());
    }

    private Class<?> defineClass(String className, byte[] classBytes) throws Exception {
        return NewClassAppender.defineClass(classLoader, className, classBytes, 0, classBytes.length,
                Object.class.getProtectionDomain());
    }
}
