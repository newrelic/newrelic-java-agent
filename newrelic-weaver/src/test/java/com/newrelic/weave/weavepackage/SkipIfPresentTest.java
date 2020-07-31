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
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for @SkipIfPresent annotation in WeavePackage.
 */
public class SkipIfPresentTest {

    public static WeavePackage noSkipIfPresent;
    public static WeavePackage skipIfPresent;

    @BeforeClass
    public static void init() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.MyWeaveExact"));
        WeavePackageConfig config1 = WeavePackageConfig.builder().name("weave_unittest_skip_if_present1").version(
                1f).source("com.newrelic.weave.weavepackage.testclasses").build();
        noSkipIfPresent = new WeavePackage(config1, weaveBytes);


        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.weavepackage.testclasses.SkipIfPresentWeave"));
        WeavePackageConfig config2 = WeavePackageConfig.builder().name("weave_unittest_skip_if_present2").version(
                1f).source("com.newrelic.weave.weavepackage.testclasses").build();
        skipIfPresent = new WeavePackage(config2, weaveBytes);

    }

    @Test
    public void testSkip() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        ClassCache cache = new ClassCache(new ClassLoaderFinder(classloader));
        Assert.assertTrue(noSkipIfPresent.validate(cache).succeeded());
        Assert.assertFalse(skipIfPresent.validate(cache).succeeded());
    }
}
