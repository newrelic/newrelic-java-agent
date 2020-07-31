/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pseudo class loader for a single class that caches the class to avoid calling {@link ClassLoader#loadClass(String)}
 * on the real class loader since this method is usually synchronized and can cause contention for the monitor.
 */
public class SingleClassLoader {

    static final int DEFAULT_MAX_SIZE = 50;

    private Map<ClassLoader, Class<?>> classMap = new ConcurrentHashMap<>();
    private final String className;
    private final int maxSize;

    public SingleClassLoader(String className) {
        this(className, DEFAULT_MAX_SIZE);
    }

    public SingleClassLoader(String className, int maxSize) {
        super();
        this.className = className;
        this.maxSize = maxSize;
    }

    public Class<?> loadClass(ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> clazz = classMap.get(classLoader);
        if (clazz == null) {
            clazz = classLoader.loadClass(className);
            if (classMap.size() == maxSize) {
                classMap.clear();
            }
            classMap.put(classLoader, clazz);
        }
        return clazz;
    }

    public void clear() {
        classMap.clear();
    }

    /**
     * For testing.
     */
    int getSize() {
        return classMap.size();
    }

}
