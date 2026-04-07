/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.test.marker.IBMJ9IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java21IncompatibleTest;
import com.newrelic.test.marker.Java25IncompatibleTest;
import com.newrelic.test.marker.Java26IncompatibleTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderTest {

    @Test
    public void test() throws Exception {
        BadClassLoader loader = new BadClassLoader();
        Class<?> clazz = loader.loadClass("com.newrelic.api.agent.NewRelic");
        verifyApi(clazz);
    }

    @Test
    public void test2() throws Exception {
        BadClassLoader2 loader = new BadClassLoader2();
        Class<?> clazz = loader.loadClass("com.newrelic.api.agent.NewRelic", true);
        verifyApi(clazz);
    }

    @Test
    public void testUrlClassLoader() throws Exception {
        ClassLoader loader = new URLClassLoader(new URL[0], null) {

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                throw new ClassNotFoundException(name);
            }

        };
        Class<?> clazz = loader.loadClass("com.newrelic.api.agent.NewRelic");
        verifyApi(clazz);
    }

    @Test
    public void testUrlClassLoaderNoLoadClass() throws ClassNotFoundException {
        ClassLoader loader = new URLClassLoader(new URL[0], null) {
        };
        Class<?> clazz = loader.loadClass("com.newrelic.agent.instrumentation.ClassLoaderCheck");
        Assert.assertNotNull(clazz);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testMissingNewRelicClass() throws ClassNotFoundException {
        ClassLoader loader = new URLClassLoader(new URL[0], null) {
        };
        loader.loadClass("com.newrelic.agent.instrumentation.Dude");
    }

    // Java 12 no longer lets us access the declared field
    @Test
    @Category({ IBMJ9IncompatibleTest.class, Java17IncompatibleTest.class, Java21IncompatibleTest.class, Java25IncompatibleTest.class, Java26IncompatibleTest.class })
    public void testSetSystemClassLoader() throws Exception {

        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        Field field = ClassLoader.class.getDeclaredField("scl");
        field.setAccessible(true);

        try {
            field.set(null, new BogusSystemClassLoader());

            BadClassLoader loader = new BadClassLoader();
            verifyApi(loader.loadClass("com.newrelic.api.agent.NewRelic"));
        } finally {
            field.set(null, systemClassLoader);
        }
    }

    @Trace(dispatcher = true)
    private void verifyApi(Class<?> clazz) throws Exception {
        Assert.assertNotNull(clazz);
        clazz.getMethod("ignoreTransaction").invoke(null);
        NewRelic.ignoreTransaction();
        Assert.assertTrue(Transaction.getTransaction().isIgnore());
    }

    private static class BadClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }

    }

    private static class BadClassLoader2 extends ClassLoader {

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }

    }

    private static class BogusSystemClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }

    }
}
