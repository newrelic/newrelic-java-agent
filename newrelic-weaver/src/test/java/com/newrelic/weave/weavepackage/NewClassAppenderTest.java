/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NewClassAppenderTest {
    private static final String aNewClassName = ANewClass.class.getName();
    private static final Map<String, byte[]> classBytesByClassName = new HashMap<>();

    @Before
    public void beforeTest() {
        classBytesByClassName.clear();
        classBytesByClassName.put(aNewClassName, ANewClass.bytes);
    }

    @Test
    public void testClassloaderAppender() throws Exception {
        ClassLoader classloader = new ClassLoader(null) {
        };
        Assert.assertNotNull(classBytesByClassName.get(aNewClassName));

        Assert.assertFalse(WeaveTestUtils.isClassLoaded(classloader, aNewClassName));
        NewClassAppender.appendClasses(classloader, classBytesByClassName);
        NewClassAppender.appendClasses(classloader, classBytesByClassName);
        NewClassAppender.appendClasses(classloader, classBytesByClassName);
        Assert.assertTrue(WeaveTestUtils.isClassLoaded(classloader, aNewClassName));
    }

    @Test
    public void testDefinedInParentClassLoader() throws Exception {
        ClassLoader parent = new ClassLoader(null) {
        };
        ClassLoader child = new ClassLoader(parent) {
        };

        Assert.assertFalse(WeaveTestUtils.isClassLoaded(parent, aNewClassName));
        NewClassAppender.appendClasses(parent, classBytesByClassName);
        Assert.assertTrue(WeaveTestUtils.isClassLoaded(parent, aNewClassName));
        Class<?> parentClass = parent.loadClass(aNewClassName);
        Class<?> childClassBeforeAppend = child.loadClass(aNewClassName);

        Assert.assertFalse(WeaveTestUtils.isClassLoaded(child, aNewClassName));
        NewClassAppender.appendClasses(child, classBytesByClassName);
        Assert.assertTrue(WeaveTestUtils.isClassLoaded(child, aNewClassName));
        Class<?> childClassAfterAppend = child.loadClass(aNewClassName);

        // Since we directly invoked defineClass for childClass2, normal classloader delegation does not occur and it
        // differs from parentClass. bug or feature?
        Assert.assertSame(parentClass, childClassBeforeAppend);
        Assert.assertNotSame(childClassBeforeAppend, childClassAfterAppend);
    }

    public static class ANewClass {
        public static byte[] bytes = null;
        static {
            try {
                bytes = WeaveUtils.getClassBytesFromClassLoaderResource(aNewClassName, Thread.currentThread().getContextClassLoader());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
