/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.weavepackage.CachedWeavePackage;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageManager;

public class TestCachedWeavePackage {
    public static WeavePackageManager manager;

    @BeforeClass
    public static void init() throws Exception {
        FunctionalWeaveTestUtils.loadWeaveTestInstrumentation();
        manager = ServiceFactory.getClassTransformerService().getContextManager().getClassWeaverService().getWeavePackageManger();
    }

    /**
     * Test to make sure internal WeavePackage caching is working.
     */
    @Test
    public void testCachedWeavePackage() {
        WeavePackage internalPackage = manager.getWeavePackage("com.newrelic.instrumentation.servlet-2.4");
        Assert.assertNotNull(internalPackage);
        Assert.assertTrue("Class type is " + internalPackage.getClass().getName(), internalPackage instanceof CachedWeavePackage);

        WeavePackage externalPackage = manager.getWeavePackage("Weave Test");
        Assert.assertNotNull(externalPackage);
        Assert.assertFalse(externalPackage instanceof CachedWeavePackage);
    }
}
