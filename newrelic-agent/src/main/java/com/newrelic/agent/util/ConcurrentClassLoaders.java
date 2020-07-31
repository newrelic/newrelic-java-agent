/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentClassLoaders {
    private static final Map<String, SingleClassLoader> classloaders = new ConcurrentHashMap<>();

    public static Class<?> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        SingleClassLoader cl = classloaders.get(className);
        if (cl == null) {
            cl = new SingleClassLoader(className);
            classloaders.put(className, cl);
        }
        return cl.loadClass(classLoader);
    }
}
