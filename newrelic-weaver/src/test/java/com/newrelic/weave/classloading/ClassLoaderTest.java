/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.classloading;

import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.classloading.CustomClassLoader.Strategy;
import com.newrelic.weave.classloading.testclasses.Bar;
import com.newrelic.weave.classloading.testclasses.Foo;
import com.newrelic.weave.utils.ClassFinder;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.JarUtils;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClassLoaderTest {
    private static URL[] classpath;

    @BeforeClass
    public static void init() throws Throwable {
        classpath = new URL[] { WeaveTestUtils.createJarFile("ClassLoaderTest", Foo.class, Bar.class) };
    }

    // @Test // this does not pass yet
    public void testInvisibleResources() throws Exception {
        Strategy classLoadingStrategy = Strategy.PARENT_FIRST;
        Strategy resourceLoadingStrategy = Strategy.HIDDEN;
        ClassLoader invisibleClassLoader = new CustomClassLoader(classLoadingStrategy, resourceLoadingStrategy, classpath, null);
        ClassLoaderUtils.runOnClassLoader(invisibleClassLoader, new TestInvisibleCallable());

        ClassFinder finder = new ClassLoaderFinder(invisibleClassLoader);
        Assert.assertTrue(ClassLoaderUtils.isClassLoadedOnClassLoader(invisibleClassLoader, Foo.class.getName()));
        Assert.assertNotNull(finder.findResource(Foo.class.getName()));

        Assert.assertFalse(ClassLoaderUtils.isClassLoadedOnClassLoader(invisibleClassLoader, Bar.class.getName()));
        Assert.assertNotNull(finder.findResource(Bar.class.getName()));
        Assert.assertFalse(ClassLoaderUtils.isClassLoadedOnClassLoader(invisibleClassLoader, Bar.class.getName()));
        invisibleClassLoader.loadClass(Bar.class.getName());
        Assert.assertTrue(ClassLoaderUtils.isClassLoadedOnClassLoader(invisibleClassLoader, Bar.class.getName()));
    }

    public static class TestInvisibleCallable implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            Foo.class.getName(); // load Foo
            return null;
        }
    }

    @Test
    public void testSelfFirst() throws IOException {
        URL[] parentClassPath = new URL[] { WeaveTestUtils.createJarFile("ParentClassLoader", Foo.class, Bar.class) };
        URL[] childClassPath = new URL[] { WeaveTestUtils.createJarFile("ChildClassLoader", Foo.class, Bar.class) };
        Strategy classLoadingStrategy = Strategy.SELF_FIRST;
        Strategy resourceLoadingStrategy = Strategy.SELF_FIRST;
        ClassLoader parentLoader = new CustomClassLoader(classLoadingStrategy, resourceLoadingStrategy, parentClassPath, null);
        ClassLoader childLoader = new CustomClassLoader(classLoadingStrategy, resourceLoadingStrategy, childClassPath, parentLoader);

        ClassFinder parentFinder = new ClassLoaderFinder(parentLoader);
        ClassFinder childFinder = new ClassLoaderFinder(childLoader);

        Assert.assertNotEquals(parentFinder.findResource(Foo.class.getName()), childFinder.findResource(Foo.class.getName()));
    }

    @Test
    public void testParentFirst() throws IOException {
        URL[] parentClassPath = new URL[] { WeaveTestUtils.createJarFile("ParentClassLoader", Foo.class, Bar.class) };
        URL[] childClassPath = new URL[] { WeaveTestUtils.createJarFile("ChildClassLoader", Foo.class, Bar.class) };
        Strategy classLoadingStrategy = Strategy.PARENT_FIRST;
        Strategy resourceLoadingStrategy = Strategy.PARENT_FIRST;
        ClassLoader parentLoader = new CustomClassLoader(classLoadingStrategy, resourceLoadingStrategy, parentClassPath, null);
        ClassLoader childLoader = new CustomClassLoader(classLoadingStrategy, resourceLoadingStrategy, childClassPath, parentLoader);

        ClassFinder parentFinder = new ClassLoaderFinder(parentLoader);
        ClassFinder childFinder = new ClassLoaderFinder(childLoader);

        Assert.assertEquals(parentFinder.findResource(Foo.class.getName()), childFinder.findResource(Foo.class.getName()));
    }

    @Test
    public void testConcurrentFinding() throws Exception {
        final int numClasses = 300;
        final int numClassesToLoad = 50; // load the first n classes
        Assert.assertTrue(numClassesToLoad <= numClasses);
        final String nameBaseInternal = "my/generated/Class";
        final String nameBaseBinary = WeaveUtils.getClassBinaryName(nameBaseInternal);
        Map<String, byte[]> bytes = new HashMap<>(numClasses);
        for (int i = 0; i < numClasses; ++i) {
            byte[] generatedBytes = ClassLoaderUtils.generateEmptyClass(nameBaseInternal + i);
            bytes.put(nameBaseBinary + i, generatedBytes);
        }
        URL[] classpath = new URL[] { JarUtils.createJarFile("testConcurrentFinding", bytes, null).toURI().toURL() };
        Assert.assertNotNull(classpath[0]);
        Strategy classLoadingStrategy = Strategy.PARENT_FIRST;
        Strategy resourceLoadingStrategy = Strategy.PARENT_FIRST;
        final ClassLoader normalClassLoader = new CustomClassLoader(classLoadingStrategy, resourceLoadingStrategy,
                classpath, null);
        Assert.assertTrue(Object.class == normalClassLoader.loadClass("java.lang.Object"));

        final int finderJobs = 500;
        final int concurrencyLevel = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        List<Future<?>> futures = new ArrayList<>(finderJobs);
        for (int i = 0; i < finderJobs; ++i) {
            if (i < numClassesToLoad) {
                Class<?> c = normalClassLoader.loadClass(nameBaseBinary + i);
            }
            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    ClassLoaderFinder finder = new ClassLoaderFinder(normalClassLoader);
                    for (int j = 0; j < numClasses; ++j) {
                        Assert.assertNotNull(finder.findResource(nameBaseBinary + j));
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        for (int i = 0; i < numClasses; ++i) {
            if (i < numClassesToLoad) {
                Assert.assertTrue(ClassLoaderUtils.isClassLoadedOnClassLoader(normalClassLoader, nameBaseBinary + i));
            } else {
                Assert.assertFalse(ClassLoaderUtils.isClassLoadedOnClassLoader(normalClassLoader, nameBaseBinary + i));
            }
        }
    }
}
