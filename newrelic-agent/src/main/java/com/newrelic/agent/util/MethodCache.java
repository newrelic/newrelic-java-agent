/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that caches @link{java.lang.reflect.Method} for classes.
 */
public class MethodCache {

    static final int DEFAULT_MAX_SIZE = 100;

    private final Map<Class<?>, Method> methods = new ConcurrentHashMap<>();
    private final String methodName;
    private final Class<?>[] parameterTypes;
    private final int maxSize;

    public MethodCache(String methodName, Class<?>... parameterTypes) {
        this(DEFAULT_MAX_SIZE, methodName, parameterTypes);
    }

    public MethodCache(int maxSize, String methodName, Class<?>... parameterTypes) {
        this.maxSize = maxSize;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    public Method getDeclaredMethod(Class<?> clazz) throws NoSuchMethodException {
        Method method = methods.get(clazz);
        if (method == null) {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            if (methods.size() == maxSize) {
                methods.clear();
            }
            methods.put(clazz, method);
        }
        return method;
    }

    public Method getDeclaredMethod(Class<?> clazz, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = methods.get(clazz);
        if (method == null) {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            if (methods.size() == maxSize) {
                methods.clear();
            }
            methods.put(clazz, method);
        }
        return method;
    }

    public Method getMethod(Class<?> clazz) throws NoSuchMethodException {
        Method method = methods.get(clazz);
        if (method == null) {
            method = clazz.getMethod(methodName, parameterTypes);
            if (methods.size() == maxSize) {
                methods.clear();
            }
            methods.put(clazz, method);
        }
        return method;
    }

    public void clear() {
        methods.clear();
    }

    /**
     * For testing.
     */
    int getSize() {
        return methods.size();
    }

}
