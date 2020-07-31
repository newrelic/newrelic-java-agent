/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import java.net.URLClassLoader;

/**
 * ClassLoader that intercepts class loads, allowing classes to be transformed.
 */
class TransformingClassLoader extends URLClassLoader {
    private static final String[] ALLOWED_PREFIXES = new String[] { "com.sun.jersey" };
    private static final String[] PROTECTED_PREFIXES = new String[] { "java.", "javax.", "com.sun.", "sun.",
            "org.junit.", "junit.framework", "com.newrelic", "org.xml", "org.w3c" };


    private static final String[] INTROSPECTOR_MUST_LOADS = new String[] {
            // This class needs to be woven.
            "com.newrelic.agent.introspec.internal.HttpTestServerImpl",

            // These classes both trigger the HttpTestServerImpl to get loaded
            "com.newrelic.agent.introspec.internal.HttpServerRule",
            "com.newrelic.agent.introspec.internal.HttpServerLocator"
    };

    public TransformingClassLoader(URLClassLoader parent) {
        super(parent.getURLs(), parent);

        try {
            // We need these classes to be loaded by this classloader.
            for (String mustLoadClassName : INTROSPECTOR_MUST_LOADS) {
                this.loadClass(mustLoadClassName, true);
            }
        } catch (ClassNotFoundException e) {
        }
    }

    protected boolean canTransform(String className) {
        for (String mustLoadClassPrefix : INTROSPECTOR_MUST_LOADS) {
            if (className.startsWith(mustLoadClassPrefix)) {
                return true;
            }
        }
        for (String prefix : ALLOWED_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        for (String prefix : PROTECTED_PREFIXES) {
            if (className.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final Class<?> loadClass(String className) throws ClassNotFoundException {

        if (canTransform(className)) {
            Class<?> alreadyLoadedClass = findLoadedClass(className);
            if (alreadyLoadedClass != null) {
                return alreadyLoadedClass;
            }

            try {
                byte[] transformedBytes = transform(className);
                if (transformedBytes != null) {
                    return defineClass(className, transformedBytes, 0, transformedBytes.length);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return findClass(className);
        }
        return super.loadClass(className);
    }

    protected byte[] transform(String className) throws Exception {
        return null;
    }
}
