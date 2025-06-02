/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Set;

/**
 * Singleton providing the appropriate {@link ClassFinder} implementation for finding class URLs from the bootstrap.
 */
public abstract class BootstrapLoader implements ClassFinder {

    public static final ClassLoader PLACEHOLDER = new ClassLoader(null) {
        @Override
        public String toString() {
            return "BootstrapPlaceholder";
        }
    };

    /**
     * @deprecated Please use {@link BootstrapLoader#PLACEHOLDER}.
     */
    @Deprecated
    public static final ClassLoader PLACEHOLER = PLACEHOLDER;

    private static final BootstrapLoader loader = create();

    /**
     * The singleton instance of the loader for the current JVM.
     *
     * @return singleton instance of the loader for the current JVM
     */
    public static BootstrapLoader get() {
        return loader;
    }

    /**
     * Create the instance of the loader for the current JVM.
     *
     * @return the instance of the loader for the current JVM
     */
    private static BootstrapLoader create() {
        try {
            return new BootstrapLoaderImpl();
        } catch (Exception e) {
            try {
                return new Java9BootstrapLoader();
            } catch (Exception ex) {
                try {
                    return new IBMBootstrapLoader();
                } catch (Exception exc) {

                    return new BootstrapLoader() {

                        @Override
                        public URL findResource(String name) {
                            return null;
                        }

                        @Override
                        public boolean isBootstrapClass(String internalName) {
                            return internalName.startsWith("java.") || internalName.startsWith("java/");
                        }
                    };
                }
            }
        }
    }

    /**
     * Returns true if the given class is a bootstrap class.
     *
     * @param internalOrClassName The internal or class name of the class.
     * @return true if the given class is a bootstrap class
     */
    public boolean isBootstrapClass(String internalOrClassName) {
        URL bootstrapResource = findResource(WeaveUtils.getClassResourceName(internalOrClassName));
        return bootstrapResource != null; // FIXME should bootstrapResource be not null??
    }

    @Override
    public abstract URL findResource(String internalOrClassName);

    /**
     * This implementation tries to determine if a requested resource is a bootstrap resource by calling the private
     * static ClassLoader method findResource.
     */
    private static class BootstrapLoaderImpl extends BootstrapLoader {
        private final java.lang.reflect.Method getBootstrapResourceMethod;

        private BootstrapLoaderImpl() throws NoSuchMethodException, SecurityException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            getBootstrapResourceMethod = ClassLoader.class.getDeclaredMethod("getBootstrapResource", String.class);
            getBootstrapResourceMethod.setAccessible(true);
            getBootstrapResourceMethod.invoke(null, "dummy");
        }

        @Override
        public URL findResource(String internalOrClassName) {
            try {
                return (URL) getBootstrapResourceMethod.invoke(null,
                        WeaveUtils.getClassResourceName(internalOrClassName));
            } catch (Exception e) {
                return null;
            }
        }

    }

    /**
     * An implementation of {@link BootstrapLoader} for IBM JVMs.
     */
    private static class IBMBootstrapLoader extends BootstrapLoader {
        // Some IBM jvms use a "systemClassLoader" field in place of the "bootstrapClassLoader" field
        private static final Set<String> BOOTSTRAP_CLASSLOADER_FIELDS = ImmutableSet.of(
                "bootstrapClassLoader", "systemClassLoader");

        private final ClassLoader bootstrapLoader;

        public IBMBootstrapLoader() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
            Field field = getBootstrapField();
            field.setAccessible(true);
            ClassLoader cl = (ClassLoader) field.get(null);
            this.bootstrapLoader = cl;
        }

        private Field getBootstrapField() throws NoSuchFieldException {
            for (String fieldName : BOOTSTRAP_CLASSLOADER_FIELDS) {
                try {
                    Field field = ClassLoader.class.getDeclaredField(fieldName);
                    return field;
                } catch (NoSuchFieldException | SecurityException e) {
                }
            }
            throw new NoSuchFieldException(MessageFormat.format("No bootstrap fields found: {0}",
                    BOOTSTRAP_CLASSLOADER_FIELDS));
        }

        @Override
        public URL findResource(String name) {
            return bootstrapLoader.getResource(WeaveUtils.getClassResourceName(name));
        }

    }

    private static class Java9BootstrapLoader extends BootstrapLoader {

        private Method methodBootLoaderFindResource;
        private ClassLoader platformClassLoader;

        private Java9BootstrapLoader() throws Exception {
            try {
                Class<?> bootLoader = Class.forName("jdk.internal.loader.BootLoader");
                methodBootLoaderFindResource = bootLoader.getDeclaredMethod("findResource", String.class);
                methodBootLoaderFindResource.invoke(null, "dummy");
            } catch (Throwable t) {
                // If we are unable to reflect on the internal BootLoader (because something went wrong with module
                // retransformation or we're in a unit test, we should clear out this reflection Method and attempt
                // to use the PlatformClassLoader as a fallback since we can invoke getResource without reflection.
                methodBootLoaderFindResource = null;

                Class classLoaderClass = ClassLoader.class;
                Method getPlatformClassLoader = classLoaderClass.getDeclaredMethod("getPlatformClassLoader");
                // If we reach this point we want to throw an uncaught exception so that the BootstrapLoader create
                // method will catch it and change class loaders
                platformClassLoader = (ClassLoader) getPlatformClassLoader.invoke(null);
            }
        }

        @Override
        public URL findResource(String internalOrClassName) {
            try {
                if (methodBootLoaderFindResource != null) {
                    return (URL) methodBootLoaderFindResource.invoke(null,
                            WeaveUtils.getClassResourceName(internalOrClassName));
                } else if (platformClassLoader != null) {
                    return platformClassLoader.getResource(WeaveUtils.getClassResourceName(internalOrClassName));
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
