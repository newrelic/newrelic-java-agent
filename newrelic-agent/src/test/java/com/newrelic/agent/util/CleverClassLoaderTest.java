/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Assert;

import org.junit.Test;

public class CleverClassLoaderTest {

    @Test
    public void loadClass() throws ClassNotFoundException {

        ClassLoader cl = new URLClassLoader(new URL[0], null);

        try {
            Class<?> clazz = cl.loadClass(CleverClassLoaderTest.class.getName());
            Assert.fail();
        } catch (ClassNotFoundException e) {
        }

        cl = new CleverClassLoader(null);

        Class<?> clazz = cl.loadClass(CleverClassLoaderTest.class.getName());

        Assert.assertEquals(CleverClassLoaderTest.class, clazz);
    }
}
