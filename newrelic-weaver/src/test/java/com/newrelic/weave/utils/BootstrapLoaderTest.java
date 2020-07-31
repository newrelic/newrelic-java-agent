/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class BootstrapLoaderTest {

    @Test
    public void testBootstrapLookups() throws IOException {
        bsLookupAsserts("java/sql/ResultSet", true);
        bsLookupAsserts("java/sql/ResultSet.class", true);
        bsLookupAsserts("java.sql.ResultSet", true);
        bsLookupAsserts(BootstrapLoaderTest.class.getName(), false);
        bsLookupAsserts(BootstrapLoaderTest.class.getName().replace('.', '/'), false);

    }

    private void bsLookupAsserts(String classOrInternalName, boolean isBootstrap) throws IOException {
        BootstrapLoader bs = BootstrapLoader.get();
        ClassCache bscache = new ClassCache(bs);
        
        Assert.assertTrue(bs.isBootstrapClass(classOrInternalName) == isBootstrap);

        if (isBootstrap) {
            Assert.assertNotNull(bs.findResource(classOrInternalName));
            Assert.assertNotNull(bscache.getClassResource(classOrInternalName));
            Assert.assertNotNull(bscache.getClassInformation(classOrInternalName));
        } else {
            Assert.assertNull(bs.findResource(classOrInternalName));
            Assert.assertNull(bscache.getClassResource(classOrInternalName));
            Assert.assertNull(bscache.getClassInformation(classOrInternalName));
        }
    }

}
