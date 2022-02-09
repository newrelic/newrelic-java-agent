/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassLoaderFinder;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is a test to make sure that using {@link WeavePackage} is thread safe. The underlying asm {@link ClassNode} is
 * not thread safe, even for reading.
 * <p/>
 * Test failures here should not be dismissed as flickers.
 */
public class ConcurrentWeaveTest {
    public static WeavePackage testPackage;
    public static ClassNode errorHandler;
    static {
        try {
            errorHandler = WeaveTestUtils.getErrorHandler();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    public static WeavePackage makeWeavePackage(int postfix) throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.ShadowedWeaveClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.ShadowedBaseClass"));

        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveExact"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveBase"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveInterface"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.WeaveUtilityClass"));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest" + postfix).source(
                "com.newrelic.weave.weavepackage.testclasses").errorHandleClassNode(errorHandler).build();
        return new WeavePackage(config, weaveBytes);
    }

    /**
     * Same weave package with multiple classloaders weaving.
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws ClassNotFoundException
     */
    @Test
    public void testConcurrentWeavesMultipleClassLoaders() throws IOException, InterruptedException,
            ExecutionException, ClassNotFoundException {
        int numClassLoaders = 10;
        List<ClassLoader> testClassLoaders = new ArrayList<>(numClassLoaders);

        for (int i = 0; i < numClassLoaders; ++i) {
            ClassLoader cl = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
            };
            testClassLoaders.add(cl);
        }

        List<Future> futures = new ArrayList<>(numClassLoaders);
        ExecutorService executor = Executors.newFixedThreadPool(numClassLoaders);
        final WeavePackage testPackage = makeWeavePackage(0);
        for (int i = 0; i < numClassLoaders; ++i) {
            final ClassLoader testLoader = testClassLoaders.get(i);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 100; ++i) {
                            testWeave(testPackage, testLoader);
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
    }

    /**
     * Same weave package with a single classloader loading multiple classes in parallel.
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws ClassNotFoundException
     */
    @Test
    public void testConcurrentWeavesMultipleClasses() throws IOException, InterruptedException, ExecutionException,
            ClassNotFoundException {
        final int numThreads = 5;
        final ClassLoader classloader = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
        };

        List<Future> futures = new ArrayList<>(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final WeavePackage testPackage = makeWeavePackage(0);
        for (int i = 0; i < numThreads; ++i) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 10; ++i) {
                            testWeave(testPackage, classloader);
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
    }

    private void testWeave(WeavePackage testPackage, ClassLoader classloader) throws IOException,
            ClassNotFoundException {
        PackageValidationResult result;
        ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));
        result = testPackage.validate(cache);

        Assert.assertFalse(result.weave("", new String[0], new String[0],
                WeaveTestUtils.getClassBytes(String.class.getCanonicalName()), cache, Collections.emptyMap()).weavedClass());
        WeaveTestUtils.expectViolations(result.getViolations());
        
        // This is here to tease out a ClassNode race condition with multiple classloaders sharing the same utility classes
        for (int i = 0; i < 100; i++) {
            Map<String, byte[]> utilityClassBytes = result.computeUtilityClassBytes(cache);
            Assert.assertNotNull(utilityClassBytes);
        }

        try {
            WeaveTestUtils.loadUtilityClasses(classloader, result.computeUtilityClassBytes(cache));
        } catch (Exception e) {
            Assert.fail("Failed with exception: " + e.getMessage());
        }

        // exact
        PackageWeaveResult packageWeaveResult = result.weave(
          "com/newrelic/weave/weavepackage/testclasses/MyOriginalExact", new String[0], new String[0],
          WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalExact"), cache,
          Collections.emptyMap());
        Assert.assertTrue(packageWeaveResult.weavedClass());

        packageWeaveResult = result.weave("com/newrelic/weave/weavepackage/testclasses/MyOriginalBase", new String[0],
                new String[0],
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"), cache, Collections.emptyMap());
        Assert.assertTrue(packageWeaveResult.weavedClass());

        String[] superClasses = { "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase" };
        String[] interfaces = { "com/newrelic/weave/weavepackage/testclasses/MyOriginalInterface" };

        // super
        packageWeaveResult = result.weave("com/newrelic/weave/weavepackage/testclasses/MyOriginalTarget1",
                superClasses, interfaces,
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget1"), cache, Collections.emptyMap());
        Assert.assertTrue(packageWeaveResult.weavedClass());

        // interface
        packageWeaveResult = result.weave("com/newrelic/weave/weavepackage/testclasses/MyOriginalTarget2",
                new String[0], interfaces,
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget2"), cache, Collections.emptyMap());
        Assert.assertTrue(packageWeaveResult.weavedClass());
    }
}
