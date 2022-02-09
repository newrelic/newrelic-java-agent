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
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.testclasses.MyOriginalBase;
import com.newrelic.weave.weavepackage.testclasses.MyOriginalExact;
import com.newrelic.weave.weavepackage.testclasses.MyOriginalInterface;
import com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget1;
import com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget2;
import com.newrelic.weave.weavepackage.testclasses.ShadowedBaseClass;
import com.newrelic.weave.weavepackage.testclasses.TestJarFile;
import com.newrelic.weave.weavepackage.testclasses.WeaveUtilityClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WeavePackageTest {
    public static WeavePackage testPackage;
    public static ClassLoader classloader;

    @BeforeClass
    public static void init() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.ShadowedWeaveClass"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.ShadowedBaseClass"));

        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveExact"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveBase"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveInterface"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.WeaveUtilityClass"));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        testPackage = new WeavePackage(config, weaveBytes);

        classloader = Thread.currentThread().getContextClassLoader();
    }

    @Test
    public void testProcessing() throws IOException {
        Assert.assertTrue(testPackage.getExactWeaves().containsKey("com/newrelic/weave/weavepackage/testclasses/ShadowedWeaveClass"));
        Assert.assertTrue(testPackage.getBaseWeaves().containsKey("com/newrelic/weave/weavepackage/testclasses/ShadowedBaseClass"));

        Assert.assertTrue(testPackage.getExactWeaves().containsKey("com/newrelic/weave/weavepackage/testclasses/MyOriginalExact"));
        Assert.assertTrue(testPackage.getBaseWeaves().containsKey("com/newrelic/weave/weavepackage/testclasses/MyOriginalBase"));
        Assert.assertTrue(testPackage.getBaseWeaves().containsKey("com/newrelic/weave/weavepackage/testclasses/MyOriginalInterface"));
        Assert.assertTrue(testPackage.getUtilClasses().containsKey("com/newrelic/weave/weavepackage/testclasses/WeaveUtilityClass"));
    }

    @Test
    public void testWeave() throws IOException, ClassNotFoundException {

        ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));
        PackageValidationResult result = testPackage.validate(cache);
        WeaveTestUtils.expectViolations(result);
        WeaveTestUtils.loadUtilityClasses(classloader, result.computeUtilityClassBytes(cache));

        Assert.assertFalse(result.weave("", new String[0], new String[0],
                                        WeaveTestUtils.getClassBytes(String.class.getCanonicalName()), cache,
                                        Collections.emptyMap()).weavedClass());

        // exact
        byte[] compositeBytes = result.weave("com/newrelic/weave/weavepackage/testclasses/MyOriginalExact",
                new String[0], new String[0],
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalExact"), cache,
                                             Collections.emptyMap()).getCompositeBytes(
                cache);
        Assert.assertNotNull(compositeBytes);
        WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.MyOriginalExact",
                compositeBytes);
        Assert.assertTrue(new MyOriginalExact().isWeaved());
        Assert.assertEquals("newMemberField", new MyOriginalExact().getMemberField());
        Assert.assertEquals("newStaticField", MyOriginalExact.getStaticField());

        compositeBytes = result.weave("com/newrelic/weave/weavepackage/testclasses/MyOriginalBase", new String[0],
                new String[0],
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase"), cache,
                                      Collections.emptyMap()).getCompositeBytes(
                cache);
        Assert.assertNotNull(compositeBytes);
        WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase",
                compositeBytes);

        String[] superClasses = { "com/newrelic/weave/weavepackage/testclasses/MyOriginalBase" };
        String[] interfaces = { "com/newrelic/weave/weavepackage/testclasses/MyOriginalInterface" };

        // super
        compositeBytes = result.weave("com/newrelic/weave/weavepackage/testclasses/MyOriginalTarget1", superClasses,
                interfaces,
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget1"), cache,
                                      Collections.emptyMap()).getCompositeBytes(
                cache);
        Assert.assertNotNull(compositeBytes);
        classloader.loadClass("com.newrelic.weave.weavepackage.testclasses.MyOriginalBase");
        classloader.loadClass("com.newrelic.weave.weavepackage.testclasses.MyOriginalInterface");
        WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget1",
                compositeBytes);
        Assert.assertTrue(new MyOriginalTarget1().isBaseCallWeaved());
        Assert.assertTrue(new MyOriginalTarget1().isWeaved());
        Assert.assertTrue(new MyOriginalTarget1().isInterfaceWeaved());

        Assert.assertEquals("newMemberField", new MyOriginalTarget1().getMemberField());
        Assert.assertEquals("newStaticField", new MyOriginalTarget1().getStaticField());

        // interface
        compositeBytes = result.weave("com/newrelic/weave/weavepackage/testclasses/MyOriginalTarget2", new String[0],
                interfaces,
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget2"), cache,
                                      Collections.emptyMap()).getCompositeBytes(
                cache);
        Assert.assertNotNull(compositeBytes);
        WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.MyOriginalTarget2",
                compositeBytes);
        Assert.assertTrue(new MyOriginalTarget2().isInterfaceWeaved());
        Assert.assertEquals("newMemberField", new MyOriginalTarget2().getMemberField());
        Assert.assertEquals("newStaticField", new MyOriginalTarget2().getStaticField());
    }

    @Test
    public void testLoadUtilityClasses() throws Exception {
        Assert.assertTrue(WeaveTestUtils.isClassLoaded(classloader,
                "com.newrelic.weave.weavepackage.testclasses.WeaveUtilityClass"));
    }

    @Test
    public void testReferences() throws IOException {
        byte[] utilBytes = WeaveUtils.getClassBytesFromClassLoaderResource(
                "com.newrelic.weave.weavepackage.testclasses.WeaveUtilityClass", classloader);
        byte[] weaveBytes = WeaveUtils.getClassBytesFromClassLoaderResource(
                "com.newrelic.weave.weavepackage.testclasses.MyWeaveExact", classloader);
        List<byte[]> bytes = Arrays.asList(utilBytes, weaveBytes);
        // Set<Reference> references = Reference.create(utilNode);
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        WeavePackage refOnlyWP = new WeavePackage(config, bytes);

        Map<String, Reference> references = refOnlyWP.getReferences();
        Assert.assertFalse(references.containsKey("java/lang/String"));
        Assert.assertFalse(references.containsKey("java/lang/Object"));
        Assert.assertFalse(references.containsKey(WeaveUtilityClass.class.getCanonicalName().replace('.', '/')));
        Assert.assertFalse(references.containsKey(MyOriginalExact.class.getCanonicalName().replace('.', '/')));

        Assert.assertTrue(references.containsKey(MyOriginalInterface.class.getCanonicalName().replace('.', '/')));
        Assert.assertTrue(references.containsKey(ShadowedBaseClass.class.getCanonicalName().replace('.', '/')));
        Assert.assertTrue(references.containsKey(MyOriginalBase.class.getCanonicalName().replace('.', '/')));
        Assert.assertTrue(references.containsKey(MyOriginalTarget1.class.getCanonicalName().replace('.', '/')));

        PackageValidationResult result = refOnlyWP.validate(new ClassCache(new ClassLoaderFinder(classloader)));
        WeaveTestUtils.expectViolations(result);
        Assert.assertEquals(5, references.size());
    }

    @Test
    public void testGetImplementationTitle() throws IOException {
        Assert.assertNotNull("This test isn't valid if the testPackage doesn't have a name.", testPackage.getName());
        ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));
        PackageValidationResult result = testPackage.validate(cache);
        WeaveTestUtils.expectViolations(result);
        WeaveTestUtils.loadUtilityClasses(classloader, result.computeUtilityClassBytes(cache));
        // from a utility class
        WeaveUtilityClass util = new WeaveUtilityClass();
        Assert.assertEquals(testPackage.getName(), util.getImplementationTitle());
        // from a weave class
        MyOriginalExact exact = new MyOriginalExact();
        Assert.assertEquals(testPackage.getName(), exact.getImplementationTitle());
    }

    /**
     * Added due to bug addressed in JAVA-4770.
     */
    @Test
    public void testJarInputStream() throws Exception {
        TestJarFile testJar;
        String testName = "weave_unittest_jar";

        // Minimium viable
        testJar = new TestJarFile();
        WeavePackageConfig jarConfig = WeavePackageConfig.builder().jarInputStream(testJar.getInputStream()).source("test_source").build();

        Assert.assertEquals(testName, jarConfig.getName(), testName);
        Assert.assertEquals(1.0f, jarConfig.getVersion(), 0.0);   // Default
        Assert.assertEquals(true, jarConfig.isEnabled());         // Default

        // All the fields
        testJar = new TestJarFile(testName, testName + "_alias", "testVendorId", "1.5", "true");
        jarConfig = WeavePackageConfig.builder().jarInputStream(testJar.getInputStream()).source("test_source").build();

        Assert.assertEquals(testName, jarConfig.getName());
        Assert.assertEquals(testName + "_alias", jarConfig.getAlias());
        Assert.assertEquals("testVendorId", jarConfig.getVendorId());
        Assert.assertEquals(1.5f, jarConfig.getVersion(), 0.0);
        Assert.assertEquals(true, jarConfig.isEnabled());

        // Different Version formats
        testJar = new TestJarFile(null, null, null, "2", null);
        jarConfig = WeavePackageConfig.builder().jarInputStream(testJar.getInputStream()).source("test_source").build();
        Assert.assertEquals(2.0f, jarConfig.getVersion(), 0.0);

        testJar = new TestJarFile(null, null, null, "2.5-42", null);
        jarConfig = WeavePackageConfig.builder().jarInputStream(testJar.getInputStream()).source("test_source").build();
        Assert.assertEquals(2.5f, jarConfig.getVersion(), 0.0);

        testJar = new TestJarFile(null, null, null, "2.5-42test", null);
        jarConfig = WeavePackageConfig.builder().jarInputStream(testJar.getInputStream()).source("test_source").build();
        Assert.assertEquals(2.5f, jarConfig.getVersion(), 0.0);

        testJar = new TestJarFile(null, null, null, "2.0.1", null);
        jarConfig = WeavePackageConfig.builder().jarInputStream(testJar.getInputStream()).source("test_source").build();
        Assert.assertEquals(2.0f, jarConfig.getVersion(), 0.0);
    }

    @Test
    public void testMultiThreadedAccess() throws Exception {
        Assert.assertNotNull("This test isn't valid if the testPackage doesn't have a name.", testPackage.getName());
        final ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));

        final Map<Integer, PackageValidationResult> packageValidationResults = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            packageValidationResults.put(i, testPackage.validate(cache));
        }

        Collection<Future<?>> results = new ArrayList<>();

        // Thread pool calling validate
        ExecutorService executorService2 = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            results.add(executorService2.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        PackageValidationResult validate = testPackage.validate(cache);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }

        // Thread pool calling methods on package validation result
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            final int current = i;
            results.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    PackageValidationResult packageValidationResult = packageValidationResults.get(current);
                    Map<String, byte[]> result = packageValidationResult.computeUtilityClassBytes(cache);
                    Assert.assertNotNull(result);
                }
            }));
        }

        for (Future<?> result : results) {
            result.get();
        }
    }
}
