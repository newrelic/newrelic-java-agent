/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import org.junit.Assert;

import org.junit.Test;

public class SingleClassLoaderTest {

    @Test
    public void oneClassLoader() throws ClassNotFoundException {

        ClassLoader realClassLoader = getClass().getClassLoader();
        Class<?> clazz = getClass();
        int maxSize = 2;
        SingleClassLoader singleClassLoader = new SingleClassLoader(clazz.getName(), maxSize);
        DelegatingClassLoader cl = new DelegatingClassLoader(realClassLoader);

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl));
        Assert.assertEquals(1, cl.getLoadCount());

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl));
        Assert.assertEquals(1, cl.getLoadCount());

        Assert.assertEquals(1, singleClassLoader.getSize());
    }

    @Test
    public void moreThanOneClassLoader() throws ClassNotFoundException {

        ClassLoader realClassLoader = getClass().getClassLoader();
        Class<?> clazz = getClass();
        int maxSize = 2;
        SingleClassLoader singleClassLoader = new SingleClassLoader(clazz.getName(), maxSize);
        DelegatingClassLoader cl = new DelegatingClassLoader(realClassLoader);
        DelegatingClassLoader cl2 = new DelegatingClassLoader(realClassLoader);

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl));
        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl2));

        Assert.assertEquals(1, cl.getLoadCount());
        Assert.assertEquals(1, cl2.getLoadCount());

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl));
        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl2));

        Assert.assertEquals(1, cl.getLoadCount());
        Assert.assertEquals(1, cl2.getLoadCount());

        Assert.assertEquals(2, singleClassLoader.getSize());
    }

    @Test
    public void overLimit() throws ClassNotFoundException {

        ClassLoader realClassLoader = getClass().getClassLoader();
        Class<?> clazz = getClass();
        int maxSize = 2;
        SingleClassLoader singleClassLoader = new SingleClassLoader(clazz.getName(), maxSize);
        DelegatingClassLoader cl = new DelegatingClassLoader(realClassLoader);
        DelegatingClassLoader cl2 = new DelegatingClassLoader(realClassLoader);
        DelegatingClassLoader cl3 = new DelegatingClassLoader(realClassLoader);

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl));
        Assert.assertEquals(1, cl.getLoadCount());
        Assert.assertEquals(1, singleClassLoader.getSize());

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl2));
        Assert.assertEquals(1, cl2.getLoadCount());
        Assert.assertEquals(2, singleClassLoader.getSize());

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl3));
        Assert.assertEquals(1, cl3.getLoadCount());
        Assert.assertEquals(1, singleClassLoader.getSize());

        Assert.assertEquals(clazz, singleClassLoader.loadClass(cl));
        Assert.assertEquals(2, singleClassLoader.getSize());
        Assert.assertEquals(2, cl.getLoadCount());

    }

    private static class DelegatingClassLoader extends ClassLoader {

        private int loadCount = 0;

        DelegatingClassLoader(ClassLoader classloader) {
            super(classloader);
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            loadCount++;
            return super.loadClass(name);
        }

        public int getLoadCount() {
            return loadCount;
        }

    }

}
