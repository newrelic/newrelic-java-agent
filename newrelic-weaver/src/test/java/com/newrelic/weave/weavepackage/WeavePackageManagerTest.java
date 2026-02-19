/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.utils.BootstrapLoader;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.WeakKeyLruCache;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WeavePackageManagerTest {
    private static final String UTIL_CLASS_NAME = "com.newrelic.weave.weavepackage.testclasses.WeaveUtilityClass2";

    public static WeavePackage testPackage1;
    public static WeavePackage testPackage2;

    @BeforeClass
    public static void init() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.ShadowedWeaveClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.ShadowedBaseClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.Weave_OriginalNameInterface"));

        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveExact"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveBase"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveInterface"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.WeavePackageManagerTest$WeaveClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.WeavePackageManagerTest$OriginalCacheTestWeaveClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.WeavePackageManagerTest$NewCacheTestWeaveClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes(UTIL_CLASS_NAME));
        WeavePackageConfig config1 = WeavePackageConfig.builder().name("weave_unittest1").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        WeavePackageConfig config2 = WeavePackageConfig.builder().name("weave_unittest2").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        testPackage1 = new WeavePackage(config1, weaveBytes);
        testPackage2 = new WeavePackage(config2, weaveBytes);
    }

    /**
     * Make sure the weaving is applied to two different weave packages.
     *
     * @throws IOException
     */
    @Test
    public void testTwoWeaves() throws IOException {
        WeavePackageManager wpm = new WeavePackageManager();
        wpm.register(testPackage1);
        wpm.register(testPackage1);// should have no effect
        wpm.register(testPackage1);// should have no effect
        wpm.register(testPackage2);
        Assert.assertEquals(2, wpm.getRegisteredPackages().size());

        String internalName = "com/newrelic/weave/weavepackage/WeavePackageManagerTest$OriginalClass";
        String className = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$OriginalClass";
        byte[] compositeBytes = WeaveTestUtils.getClassBytes(className);
        Assert.assertNotNull(compositeBytes);
        // /*-
        compositeBytes = wpm.weave(Thread.currentThread().getContextClassLoader(), internalName, compositeBytes,
                                   Collections.emptyMap());
        for (PackageValidationResult res :
                wpm.validPackages.get(Thread.currentThread().getContextClassLoader()).values()) {
            WeaveTestUtils.expectViolations(res);
        }
        Assert.assertEquals(1, getCacheSize(wpm.validPackages));
        Assert.assertEquals(2, wpm.validPackages.get(Thread.currentThread().getContextClassLoader()).size());

        Assert.assertNotNull(compositeBytes);
        WeaveTestUtils.addToContextClassloader(className, compositeBytes);
        OriginalClass oc = new OriginalClass();
        Assert.assertEquals(0, oc.weaveInvokes);
        oc.aMethodToWeave();
        Assert.assertEquals(2, oc.weaveInvokes);
    }

    @Test
    public void testBootstrapWeave() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.WeavePackageManagerTest$ResultSet"));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_bootstrap").source(
                "com.newrelic.weave.weavepackage.WeavePackageManagerTest").build();
        WeavePackage bsPackage = new WeavePackage(config, weaveBytes);

        Assert.assertTrue(bsPackage.weavesBootstrap());
        WeaveTestUtils.expectViolations(bsPackage.validate(new ClassCache(BootstrapLoader.get())));

        WeavePackageManager wpm = new WeavePackageManager(null, Mockito.mock(Instrumentation.class), 10, true, true);
        Assert.assertTrue(wpm.canWeaveBootstrapClassLoader());
        wpm.register(bsPackage);

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));
        String targetClassName = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$ResultSetImpl";
        String targetInternalName = targetClassName.replace('.', '/');
        ClassInformation classInformation = cache.getClassInformation(targetInternalName);
        Assert.assertNotNull(classInformation);
        String[] superNames = classInformation.getAllSuperNames(cache).toArray(new String[0]);
        String[] interfaceNames = classInformation.getAllInterfaces(cache).toArray(new String[0]);

        Assert.assertEquals(1, wpm.match(classloader, targetInternalName, cache).size());
        byte[] result = wpm.weave(classloader, targetInternalName,
                WeaveTestUtils.getClassBytes(targetClassName), Collections.emptyMap());
        Assert.assertTrue(bsPackage.hasMatcher(targetInternalName, superNames, interfaceNames,
                Collections.<String>emptySet(), Collections.<String>emptySet(), null));
        Assert.assertEquals(0, getCacheSize(wpm.invalidPackages));
        Assert.assertEquals(1, getCacheSize(wpm.validPackages));
        Assert.assertNotNull(wpm.validPackages.get(BootstrapLoader.PLACEHOLDER));
        Assert.assertNotNull(wpm.validPackages.get(BootstrapLoader.PLACEHOLDER).get(bsPackage));

        Assert.assertNotNull(result);
    }

    @Test
    public void testMaxValidClassLoaders() throws IOException {
        TestListener listener = new TestListener();
        WeavePackageManager wpm = new WeavePackageManager(listener);
        wpm.register(testPackage1);

        // 1) load a class with the original classloader and verify that a class loads and is weaved
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        ClassLoader originalClassLoader = new ClassLoader(context) {
        };
        String internalName = "com/newrelic/weave/weavepackage/WeavePackageManagerTest$OriginalCacheTestClass";
        String className = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$OriginalCacheTestClass";
        byte[] compositeBytes = WeaveTestUtils.getClassBytes(className);
        Assert.assertNotNull(compositeBytes);
        compositeBytes = wpm.weave(originalClassLoader, internalName, compositeBytes, Collections.emptyMap());
        Assert.assertEquals(1, getCacheSize(wpm.validPackages));
        Assert.assertEquals(1, wpm.validPackages.get(originalClassLoader).size());

        Assert.assertNotNull(compositeBytes);
        WeaveTestUtils.addToContextClassloader(className, compositeBytes);
        OriginalCacheTestClass octc = new OriginalCacheTestClass();
        Assert.assertEquals(0, octc.originalCacheTestWeaveInvokes);
        octc.anOriginalMethodToWeave();
        Assert.assertEquals(1, octc.originalCacheTestWeaveInvokes);

        // 2) load up 200 (100 * 2) classloaders (the maximum size of the cache).
        // This will push the original classloader out of the cache
        final int numClassLoaders = WeavePackageManager.MAX_VALID_PACKAGE_CACHE * 2;
        List<ClassLoader> classloaders = new ArrayList<>(numClassLoaders);
        for (int i = 0; i < numClassLoaders; ++i) {
            ClassLoader cl = new ClassLoader(context) {
            };
            classloaders.add(cl);

            wpm.weave(cl, "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                      WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                      Collections.emptyMap());
        }
        Assert.assertTrue(getCacheSize(wpm.validPackages) <= WeavePackageManager.MAX_VALID_PACKAGE_CACHE);

        // 3) load the original class and a new class with the original classloader and verify they load + weave

        // Original class
        compositeBytes = wpm.weave(originalClassLoader, internalName, compositeBytes, Collections.emptyMap());
        Assert.assertNotNull(compositeBytes);
        octc = new OriginalCacheTestClass();
        Assert.assertEquals(0, octc.originalCacheTestWeaveInvokes);
        octc.anOriginalMethodToWeave();
        Assert.assertEquals(1, octc.originalCacheTestWeaveInvokes);

        // New class
        String newInternalName = "com/newrelic/weave/weavepackage/WeavePackageManagerTest$NewCacheTestClass";
        String newClassName = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$NewCacheTestClass";
        byte[] newCompositeBytes = WeaveTestUtils.getClassBytes(newClassName);
        Assert.assertNotNull(newCompositeBytes);
        newCompositeBytes = wpm.weave(originalClassLoader, newInternalName, newCompositeBytes, Collections.emptyMap());
        Assert.assertTrue(getCacheSize(wpm.validPackages) <= WeavePackageManager.MAX_VALID_PACKAGE_CACHE);
        Assert.assertEquals(1, wpm.validPackages.get(originalClassLoader).size());

        Assert.assertNotNull(newCompositeBytes);
        WeaveTestUtils.addToContextClassloader(newClassName, newCompositeBytes);
        NewCacheTestClass nctc = new NewCacheTestClass();
        Assert.assertEquals(0, nctc.newCacheTestWeaveInvokes);
        nctc.aNewMethodToWeave();
        Assert.assertEquals(1, nctc.newCacheTestWeaveInvokes);
    }

    @Test
    public void testMaxInvalidClassLoaders() throws IOException {
        TestListener listener = new TestListener();
        WeavePackageManager wpm = new WeavePackageManager(listener);
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.WeavePackageManagerTest$NoMatchWeaveClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.WeavePackageManagerTest$NewNoMatchWeaveClass"));
        WeavePackageConfig noMatchPackage = WeavePackageConfig.builder().name("weave_nomatch").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        wpm.register(new WeavePackage(noMatchPackage, weaveBytes));

        // 1) load a class with the original classloader and verify that it loads and doesn't weave (pushes to invalid)
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        ClassLoader originalClassLoader = new ClassLoader(context) {
        };
        String internalName = "com/newrelic/weave/weavepackage/WeavePackageManagerTest$NoMatchClass";
        String className = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$NoMatchClass";
        byte[] compositeBytes = WeaveTestUtils.getClassBytes(className);
        Assert.assertNotNull(compositeBytes);
        compositeBytes = wpm.weave(originalClassLoader, internalName, compositeBytes, Collections.emptyMap());
        Assert.assertEquals(1, getCacheSize(wpm.invalidPackages));
        Assert.assertEquals(1, wpm.invalidPackages.get(originalClassLoader).size());

        Assert.assertNull(compositeBytes);
        NoMatchClass nmc = new NoMatchClass();
        Assert.assertEquals(0, nmc.noMatchWeaveInvokes);
        nmc.nonMatchingMethod();
        Assert.assertEquals(0, nmc.noMatchWeaveInvokes);

        // 2) load up 200 (100 * 2) invalid classloaders (the maximum size of the cache).
        // This will push the original classloader out of the "invalid" cache
        final int numClassLoaders = WeavePackageManager.MAX_INVALID_PACKAGE_CACHE * 2;
        List<ClassLoader> classloaders = new ArrayList<>(numClassLoaders);
        for (int i = 0; i < numClassLoaders; ++i) {
            ClassLoader cl = new ClassLoader(context) {
            };
            classloaders.add(cl);

            wpm.weave(cl, "com/newrelic/weave/weavepackage/WeavePackageManagerTest$NewNoMatchClass",
                    WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.WeavePackageManagerTest$NewNoMatchClass"),
                      Collections.emptyMap()
            );

        }
        Assert.assertTrue(WeavePackageManager.MAX_INVALID_PACKAGE_CACHE >= getCacheSize(wpm.invalidPackages));

        // 3) load with the original classloader again and verify that class loads but doesn't weave (pushes to invalid)
        compositeBytes = WeaveTestUtils.getClassBytes(className);
        compositeBytes = wpm.weave(originalClassLoader, internalName, compositeBytes, Collections.emptyMap());
        Assert.assertNull(compositeBytes);
        nmc = new NoMatchClass();
        Assert.assertEquals(0, nmc.noMatchWeaveInvokes);
        nmc.nonMatchingMethod();
        Assert.assertEquals(0, nmc.noMatchWeaveInvokes);
    }

    @Test
    public void testWeakClassLoaders() throws IOException {
        TestListener listener = new TestListener();
        WeavePackageManager wpm = new WeavePackageManager(listener);
        wpm.register(testPackage1);
        wpm.register(testPackage2);

        final int numClassLoaders = 20;
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        List<ClassLoader> classloaders = new ArrayList<>(numClassLoaders);
        for (int i = 0; i < numClassLoaders; ++i) {
            ClassLoader cl = new ClassLoader(context) {
            };
            classloaders.add(cl);

            wpm.weave(cl, "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                    WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                      Collections.emptyMap());
        }
        Assert.assertEquals(numClassLoaders, getCacheSize(wpm.validPackages));

        for (int i = 0; i < 5; ++i) {
            System.gc();
            ClassLoader cl = new ClassLoader(context) {
            };
            wpm.weave(cl, "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                    WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                      Collections.emptyMap());
        }
        Assert.assertTrue(getCacheSize(wpm.validPackages) >= numClassLoaders);

        classloaders.clear();
        for (int i = 0; i < 10; ++i) {
            System.gc();
            ClassLoader cl = new ClassLoader(context) {
            };
            wpm.weave(cl, "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                    WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                      Collections.emptyMap());
            wpm.invalidPackages.remove(cl);
            wpm.validPackages.remove(cl);
        }

        Assert.assertTrue(getCacheSize(wpm.validPackages) < numClassLoaders);
    }

    /**
     * When a new package is registered, the cache should invalidate.
     */
    @Test
    public void testClassLoaderCache() throws IOException {
        ClassLoader cl = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
        };
        WeavePackageManager wpm = new WeavePackageManager();
        byte[] result = wpm.weave(cl, new ClassCache(new ClassLoaderFinder(cl)),
                "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                                  Collections.emptyMap(), null);
        Assert.assertNull(result);
        wpm.register(testPackage1);
        result = wpm.weave(cl, new ClassCache(new ClassLoaderFinder(cl)),
                "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                           Collections.emptyMap(), null);
        Assert.assertNotNull(result);
    }

    @Test
    public void testConcurrentWeavePackage() throws Exception {
        final ExecutorService executor1 = Executors.newFixedThreadPool(5);
        final ExecutorService executor2 = Executors.newFixedThreadPool(5);
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();

        Collection<Future> futures = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            futures.add(executor1.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new parallel classloader for each run
                        final ClassLoader cl = new ParallelClassLoader(null) {
                        };

                        final WeavePackageManager wpm = new WeavePackageManager();
                        wpm.register(testPackage1);
                        final ClassCache classCache = new ClassCache(new ClassLoaderFinder(originalCl));
                        Thread.currentThread().setContextClassLoader(cl);

                        Future<byte[]> result1 = executor2.submit(new Callable<byte[]>() {
                            @Override
                            public byte[] call() throws Exception {
                                Thread.currentThread().setContextClassLoader(originalCl);
                                byte[] result = wpm.weave(cl, classCache,
                                        "com/newrelic/weave/weavepackage/testclasses/MyOriginalExact",
                                        WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalExact"),
                                                          Collections.emptyMap(), null);

                                Thread.currentThread().setContextClassLoader(cl);
                                WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.MyOriginalExact", result);
                                return result;
                            }
                        });

                        Future<byte[]> result2 = executor2.submit(new Callable<byte[]>() {
                            @Override
                            public byte[] call() throws Exception {
                                Thread.currentThread().setContextClassLoader(originalCl);
                                byte[] result = wpm.weave(cl, classCache,
                                        "com/newrelic/weave/weavepackage/testclasses/OriginalNameInterface",
                                        WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.OriginalNameInterface"),
                                                          Collections.emptyMap(), null);

                                Thread.currentThread().setContextClassLoader(cl);
                                WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.OriginalNameInterface", result);

                                Thread.currentThread().setContextClassLoader(originalCl);
                                byte[] result2 = wpm.weave(cl, classCache,
                                        "com/newrelic/weave/weavepackage/testclasses/OriginalNameInterfaceImpl",
                                        WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.OriginalNameInterfaceImpl"),
                                                           Collections.emptyMap(), null);

                                Thread.currentThread().setContextClassLoader(cl);
                                WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.OriginalNameInterfaceImpl", result2);
                                return result2;
                            }
                        });

                        Assert.assertNotNull(result1.get());
                        Class<?> myOriginalExactClass = cl.loadClass("com.newrelic.weave.weavepackage.testclasses.MyOriginalExact");
                        Object myOriginalExact = myOriginalExactClass.newInstance();
                        Assert.assertNotNull(myOriginalExact);

                        Assert.assertNotNull(result2.get());
                        Class<?> originalNameInterfaceImplClass = cl.loadClass("com.newrelic.weave.weavepackage.testclasses.OriginalNameInterfaceImpl");
                        Object originalNameInterfaceImpl = originalNameInterfaceImplClass.newInstance();
                        Assert.assertNotNull(originalNameInterfaceImpl);
                    } catch (NoClassDefFoundError cnfe) {
                        Assert.fail("A NoClassDefFoundError was encountered. This indicates a class loading regression in the WeavePackageManager.");
                    } catch (Exception e) {
                        Assert.fail("An unexpected exception occurred.");
                    }
                }
            }));
        }

        // Wait for all futures to complete
        for (Future<?> future : futures) {
            future.get();
        }
    }

    /**
     * Make sure a {@link WeavePackageLifetimeListener} is invoked at the right times.
     *
     * @throws IOException
     */
    @Test
    public void testListener() throws IOException {
        TestListener listener = new TestListener();
        int expectedInvokeCount = 0;
        WeavePackageManager wpm = new WeavePackageManager(listener);
        Assert.assertTrue(expectedInvokeCount == listener.invokeCount);
        wpm.register(testPackage1);
        expectedInvokeCount++;
        Assert.assertTrue(expectedInvokeCount == listener.invokeCount);
        wpm.register(testPackage2);
        expectedInvokeCount++;
        Assert.assertTrue(expectedInvokeCount == listener.invokeCount);

        ClassLoader cl = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
        };
        byte[] result = wpm.weave(cl, new ClassCache(new ClassLoaderFinder(cl)),
                "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                                  Collections.emptyMap(), listener);
        Assert.assertNotNull(result);
        expectedInvokeCount += 4; // two packages, each with a match and weave
        Assert.assertTrue(expectedInvokeCount == listener.invokeCount);

        byte[] result2 = wpm.weave(cl, new ClassCache(new ClassLoaderFinder(cl)),
                "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase",
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"),
                                   Collections.emptyMap(), listener);
        Assert.assertNotNull(result2);
        expectedInvokeCount += 2; // shouldn't validate again. Only weave.
        Assert.assertTrue(expectedInvokeCount == listener.invokeCount);

        wpm.deregister(testPackage1.getName());
        expectedInvokeCount++;
        Assert.assertTrue(expectedInvokeCount == listener.invokeCount);
        wpm.deregister(testPackage2.getName());
        expectedInvokeCount++;
        Assert.assertTrue(expectedInvokeCount == listener.invokeCount);
    }

    private static int getCacheSize(WeakKeyLruCache<?, ?> map) {
        // For Caffeine-backed maps, cleanup happens automatically via weak references and size eviction
        return map.size();
    }

    private static class TestListener implements WeavePackageLifetimeListener, ClassWeavedListener {
        public int invokeCount = 0;

        @Override
        public void registered(WeavePackage weavepackage) {
            invokeCount++;
        }

        @Override
        public void deregistered(WeavePackage weavepackage) {
            invokeCount++;
        }

        @Override
        public void validated(PackageValidationResult packageResult, ClassLoader classloader) {
            invokeCount++;
        }

        @Override
        public void classWeaved(PackageWeaveResult weaveResult, ClassLoader classloader, ClassCache cache) {
            invokeCount++;
        }
    }

    private static class OriginalClass {
        public int weaveInvokes = 0;

        public void aMethodToWeave() {
            "originalbody".toString();
        }
    }

    @Weave(originalName = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$OriginalClass")
    private static class WeaveClass {
        public int weaveInvokes;

        public void aMethodToWeave() {
            weaveInvokes++;
            Weaver.callOriginal();
        }
    }

    private static class OriginalCacheTestClass {
        public int originalCacheTestWeaveInvokes = 0;

        public void anOriginalMethodToWeave() {
            "originalcachetestbody".toString();
        }
    }

    @Weave(originalName = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$OriginalCacheTestClass")
    private static class OriginalCacheTestWeaveClass {
        public int originalCacheTestWeaveInvokes;

        public void anOriginalMethodToWeave() {
            originalCacheTestWeaveInvokes++;
            Weaver.callOriginal();
        }
    }

    private static class NewCacheTestClass {
        public int newCacheTestWeaveInvokes = 0;

        public void aNewMethodToWeave() {
            "newcachetestbody".toString();
        }
    }

    @Weave(originalName = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$NewCacheTestClass")
    private static class NewCacheTestWeaveClass {
        public int newCacheTestWeaveInvokes;

        public void aNewMethodToWeave() {
            newCacheTestWeaveInvokes++;
            Weaver.callOriginal();
        }
    }

    private static class NoMatchClass {
        public int noMatchWeaveInvokes = 0;

        public void nonMatchingMethod() {
            "nonmatchingmethodbody".toString();
        }
    }

    @Weave(originalName = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$NoMatchClass")
    private static class NoMatchWeaveClass {
        public int noMatchWeaveInvokes;

        public void aNewNonMatchingMethodToWeave() {
            noMatchWeaveInvokes++;
            Weaver.callOriginal();
        }
    }

    private static class NewNoMatchClass {
        public int newNoMatchWeaveInvokes = 0;

        public void newNonMatchingMethod() {
            "nonmatchingmethodbody".toString();
        }
    }

    @Weave(originalName = "com.newrelic.weave.weavepackage.WeavePackageManagerTest$NewNoMatchClass")
    private static class NewNoMatchWeaveClass {
        public int newNoMatchWeaveInvokes;

        public void aNewNonMatchingMethodToWeave() {
            newNoMatchWeaveInvokes++;
            Weaver.callOriginal();
        }
    }

    @Weave(type = MatchType.Interface, originalName = "java.sql.ResultSet")
    public static class ResultSet {
        public boolean next() {
            return Weaver.callOriginal();
        }
    }

    public abstract static class ResultSetImpl implements java.sql.ResultSet {
        public boolean next() {
            return true;
        }
    }

    private static class ParallelClassLoader extends ClassLoader {

        private ParallelClassLoader(ClassLoader parent) {
            super(parent);
            registerAsParallelCapable();
        }

    }
}
