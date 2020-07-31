/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;

/**
 * When we try to access methods through reflection in a JVM that uses a {@link SecurityManager} (when we're running in
 * WebSphere, for example), we get access exceptions when calling the reflection apis from most of the agent code. This
 * class is loaded on the bootstrap classloader and has permission to call these apis. Note that it still fails when
 * trying to invoke {@link Method#setAccessible(boolean)}.
 * 
 * Don't delete methods off of this class. While the IDE might not find direct callers, many methods are invoked through
 * rewritten instructions in weaved classes.
 */
public class ClassReflection {
    private ClassReflection() {
    }

    public static ClassLoader getClassLoader(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return clazz.getClassLoader();
            }
        });

    }

    public static Class<?> loadClass(final ClassLoader classLoader, final String name) throws ClassNotFoundException {
        NewRelic.getAgent().getLogger().log(Level.FINEST, "Loading class {0}", name);
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {

                @Override
                public Class<?> run() throws Exception {
                    if (classLoader == null) {
                        return AgentBridge.getAgent().getClass().getClassLoader().loadClass(name);
                    } else {
                        return classLoader.loadClass(name);
                    }
                }

            });
        } catch (PrivilegedActionException e) {
            throw new ClassNotFoundException("Unable to load " + name, e);
        }

    }

    /**
     * @see Class#getDeclaredMethods()
     */
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    /**
     * @see Class#getDeclaredMethod(String, Class...)
     */
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException, SecurityException {
        Method m = clazz.getDeclaredMethod(name, parameterTypes);
        return m;
    }

    /**
     * @see Class#getMethods()
     */
    public static Method[] getMethods(Class<?> clazz) {
        return clazz.getMethods();
    }

    /**
     * @see Class#getDeclaredConstructors()
     */
    public static Constructor<?>[] getDeclaredConstructors(Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    /**
     * @see Class#getDeclaredFields()
     */
    public static Field[] getDeclaredFields(Class<?> clazz) {
        return clazz.getDeclaredFields();

    }

    public static Field getDeclaredField(final Class<?> clazz, final String name) throws NoSuchFieldException,
            SecurityException {

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {

                @Override
                public Field run() throws Exception {
                    Field field = clazz.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                }
            });
        } catch (PrivilegedActionException e) {
            try {
                throw e.getCause();
            } catch (NoSuchFieldException | SecurityException ex) {
                throw ex;
            } catch (Throwable t) {
                throw new SecurityException(e);
            }
        }
    }

    public static Object get(final Field field, final Object instance) throws IllegalArgumentException,
            IllegalAccessException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

                @Override
                public Object run() throws Exception {
                    return field.get(instance);
                }

            });
        } catch (PrivilegedActionException e) {
            return handleAccessException(e.getCause());
        }

    }

    private static Void handleAccessException(Throwable cause) throws IllegalAccessException, IllegalArgumentException {
        try {
            throw cause;
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new IllegalAccessException(t.toString());
        }
    }

    public static void setAccessible(final Field field, final boolean flag) throws IllegalArgumentException,
            IllegalAccessException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                @Override
                public Void run() throws Exception {
                    field.setAccessible(flag);
                    return null;
                }

            });
        } catch (PrivilegedActionException e) {
            handleAccessException(e.getCause());
        }

    }

    public static void setAccessible(final Method method, final boolean flag) throws IllegalArgumentException,
            IllegalAccessException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                @Override
                public Void run() throws Exception {
                    method.setAccessible(flag);
                    return null;
                }

            });
        } catch (PrivilegedActionException e) {
            handleAccessException(e.getCause());
        }

    }
}
