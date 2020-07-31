/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;

import org.junit.Test;

/**
 * ClassFinderTest.java
 */
public class ClassLoaderFinderTest {

    @Test
    public void testFindResource() {
        ClassLoaderFinder finder = new ClassLoaderFinder(Thread.currentThread().getContextClassLoader());
        URL exists = finder.findResource(ClassLoaderFinder.class.getName());
        assertNotNull(exists);

        URL notExists = finder.findResource("joe.loves.Pizza");
        assertNull(notExists);
    }
}
