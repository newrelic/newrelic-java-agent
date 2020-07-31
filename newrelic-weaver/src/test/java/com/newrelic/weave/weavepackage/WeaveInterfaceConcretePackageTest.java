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
import com.newrelic.weave.weavepackage.testclasses.BearConcrete;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WeaveInterfaceConcretePackageTest {

    public static WeavePackage testPackage;
    public static ClassLoader classloader;

    @BeforeClass
    public static void init() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();

        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.WeaveBearInterface"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.WeaveUtilityClass"));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        testPackage = new WeavePackage(config, weaveBytes);

        classloader = Thread.currentThread().getContextClassLoader();
    }

    @Test
    public void testProcessing() throws IOException {
        Assert.assertTrue(
                testPackage.getBaseWeaves().containsKey("com/newrelic/weave/weavepackage/testclasses/BearInterface"));
        Assert.assertTrue(
                testPackage.getUtilClasses().containsKey("com/newrelic/weave/weavepackage/testclasses/WeaveUtilityClass"));
    }

    @Test
    public void testWeave() throws IOException, ClassNotFoundException {

        ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));
        PackageValidationResult result = testPackage.validate(cache);
        WeaveTestUtils.expectViolations(result);
        WeaveTestUtils.loadUtilityClasses(classloader, result.computeUtilityClassBytes(cache));

        Assert.assertFalse(result.weave("", new String[0], new String[0],
                WeaveTestUtils.getClassBytes(String.class.getCanonicalName()), cache).weavedClass());

        String[] superClasses = { "com/newrelic/weave/weavepackage/testclasses/BearBaseClass" };
        String[] interfaces = { "com/newrelic/weave/weavepackage/testclasses/BearInterface" };

        // super
        byte[] compositeBytes = result.weave("com/newrelic/weave/weavepackage/testclasses/BearConcrete", superClasses,
                interfaces,
                WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.BearConcrete"), cache)
                .getCompositeBytes(
                        cache);
        Assert.assertNotNull(compositeBytes);
        classloader.loadClass("com.newrelic.weave.weavepackage.testclasses.BearBaseClass");
        classloader.loadClass("com.newrelic.weave.weavepackage.testclasses.BearInterface");
        WeaveTestUtils.addToContextClassloader("com.newrelic.weave.weavepackage.testclasses.BearConcrete",
                compositeBytes);

        //We expect this to be false because this method is inherited from the super class and doesn’t actually
        // exist in the concrete class bytecode, just a bridge method that calls the super’s method
        Assert.assertFalse(new BearConcrete().isWeaved());
        Assert.assertTrue(new BearConcrete().isAlsoWeaved());
    }
}