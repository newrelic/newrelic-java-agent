/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.runners.model.InitializationError;

final class TransformingTestUtils {
    private TransformingTestUtils() {
    }

    public static Class<?> applyClassLoader(Class<?> classUnderTest, ClassLoader classLoader)
            throws InitializationError {
        try {
            Thread.currentThread().setContextClassLoader(classLoader);

            // return the test class, loaded by the new loader
            return Class.forName(classUnderTest.getName(), true, classLoader);
        } catch (Exception e) {
            throw new InitializationError(e);
        }
    }

    public static URLClassLoader getParentAsUrlClassLoader() throws InitializationError {
        /*
         * Previously we were getting the parentLoader from Thread.currentThread().getContextClassLoader() which was
         * returning the AppClassLoader. To support Java 9 we can no longer cast AppClassLoader to URLClassLoader,
         * so instead we iterate through classpath to get all URLs loaded by AppClassLoader.
         */
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);
        URL[] urls = new URL[classpathEntries.length];

        try {
            for (int i = 0; i < classpathEntries.length; i++) {
                urls[i] = new File(classpathEntries[i]).toURI().toURL();
            }
        } catch (MalformedURLException ex) {
        }

        ClassLoader parentLoader = new URLClassLoader(urls);
        return (URLClassLoader) parentLoader;
    }
}
