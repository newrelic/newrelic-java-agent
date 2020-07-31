/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.google.common.collect.Sets;
import com.newrelic.weave.utils.JarUtils;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Loads classes by injecting byte arrays into a classloader using reflective calls to
 * defineClass.
 */
public class NewClassAppender {
    // define classes with object's protection domain
    private static final ProtectionDomain PROTECTION_DOMAIN = NewClassAppender.class.getProtectionDomain();
    static final Method defineClassMethod;
    static {
        try {
            defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class,
                    int.class, int.class, ProtectionDomain.class);
            defineClassMethod.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static Method addURLMethod;
    static {
        try {
            addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURLMethod.setAccessible(true);
        } catch (Exception ex) {
            // Do not re-throw an exception here, this may be missing
        }
    }

    static Class<?> builtInClassLoaderClass;
    static Field ucpField;
    static Method urlClassPathAddURLMethod;
    static {
        try {
            Class<?> builtInClassLoader = Class.forName("jdk.internal.loader.BuiltinClassLoader");
            Class<?> urlClassPath = Class.forName("jdk.internal.loader.URLClassPath");
            if (builtInClassLoader != null && urlClassPath != null) {
                builtInClassLoaderClass = builtInClassLoader;
                ucpField = builtInClassLoader.getDeclaredField("ucp");
                ucpField.setAccessible(true);
                urlClassPathAddURLMethod = urlClassPath.getDeclaredMethod("addURL", URL.class);
            }
        } catch (Exception ex) {
            // Do not re-throw an exception here, this may be missing
        }
    }

    public static void appendClassesToBootstrapClassLoader(Instrumentation instrumentation, Map<String, byte[]> classBytesMap)
            throws IOException {
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(JarUtils.createJarFile("instrumentation",
                classBytesMap)));
    }

    /**
     * Add class using the ClassLoader.defineClass method. If that fails with duplicate class error, redefine the class.
     */
    public static void appendClasses(ClassLoader classloader, Map<String, byte[]> classBytesMap) throws IOException {
        appendClasses(null, null, null, classloader, classBytesMap);
    }

    /**
     * Add class using the ClassLoader.defineClass method. If that fails with duplicate class error, redefine the class.
     */
    static void appendClasses(String className, String superName, String[] interfaceNames, ClassLoader classloader,
            Map<String, byte[]> classBytesMap) throws IOException {
        loadClassesInClassLoader(className, superName, interfaceNames, classloader, classBytesMap);
    }

    /**
     * Define the specified class in the specified classloader.
     * Note: this is public for testing purposes only.
     */
    public static Class<?> defineClass(ClassLoader classloader, String className, byte[] classBytes, int offset,
            int length, ProtectionDomain protectionDomain) throws IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException {
        return (Class<?>) defineClassMethod.invoke(classloader, className.replace('/', '.'), classBytes, offset, length,
                protectionDomain);
    }

    /**
     * Takes the new classes and feeds them into the classloader. First it tries to bundle the bytecode into a jar and add it to the classloader
     * via the addURL mechanism. If the classloader is not a URLClassLoader it will fall back to using the defineClass method on java.lang.ClassLoader.
     *
     * This is implemented as a fallback mechanism because it has been shown to have limitations when a class in the classBytesMap extends the class
     * that is currently being loaded (a LinkageError is thrown because the class is added to the classloader twice).
     *
     * @param classloader Classloader to append bytes to.
     * @param classBytesMap Contains the new classes: (internal classname) -&gt (byte array to append).
     * @throws IOException
     */
    private static void loadClassesInClassLoader(String className, String superName, String[] interfaceNames, ClassLoader classloader,
            Map<String, byte[]> classBytesMap) throws IOException {
        className = className == null ? "" : className;
        superName = superName == null || superName.equals("java/lang/Object") ? "" : superName; // Don't match java/lang/Object
        Set<String> interfaceNamesSet = interfaceNames != null ? Sets.newHashSet(interfaceNames) : Collections.<String>emptySet();

        if (classloader instanceof URLClassLoader) {
            if (isTransformingCurrentClass(className, superName, interfaceNamesSet, classBytesMap)) {
                try {
                    // Try to load the class bytes into the classloader via the `addURL` mechanism. This will only work if the classloader is a URLClassLoader.
                    File utilityClassJar = JarUtils.createJarFile("nr-inst-utilities", classBytesMap);
                    addURLMethod.invoke(classloader, utilityClassJar.toURI().toURL());
                    return;
                } catch (Throwable t) {
                    // Allow this code to go down to the fallback mechanism below
                }
            }
        } else if (builtInClassLoaderClass != null && builtInClassLoaderClass.isInstance(classloader) && ucpField != null) {
            if (isTransformingCurrentClass(className, superName, interfaceNamesSet, classBytesMap)) {
                try {
                    // Try to load the class bytes into the classloader via the `UrlClassPath` mechanism.
                    // This will only work if the classloader is a BuiltInClassLoader.
                    File utilityClassJar = JarUtils.createJarFile("nr-inst-utilities", classBytesMap);
                    Object urlClassPath = ucpField.get(classloader);
                    if (urlClassPath != null) {
                        urlClassPathAddURLMethod.invoke(urlClassPath, utilityClassJar.toURI().toURL());
                        return;
                    }
                } catch (Throwable t) {
                    // Allow this code to go down to the fallback mechanism below
                }
            }
        }

        // If we got here it means that the addURL call above failed or never ran (because classloader
        // is not an instance of URLClassLoader). So we need to try manually defining the classes.
        boolean continueLoading = true;
        Map<String, byte[]> loadedClasses = new HashMap<>();
        Set<String> unloadedClasses = new HashSet<>(classBytesMap.keySet());

        while (continueLoading && unloadedClasses.size() > 0) {
            continueLoading = false;// If we don't load any classes on this pass, we need to stop

            for (String classname : unloadedClasses) {
                byte[] classBytes = classBytesMap.get(classname);
                if (null == classBytes) {
                    continue;
                }
                try {
                    defineClass(classloader, classname.replace('/', '.'), classBytes, 0, classBytes.length,
                            PROTECTION_DOMAIN);
                    continueLoading = true;// We loaded at least one class and can continue looping
                    loadedClasses.put(classname, classBytes);
                } catch (Throwable t) {
                    if (t.getCause() instanceof LinkageError) {
                        String errorMessage = t.getCause().getMessage();
                        if (errorMessage != null && errorMessage.contains("duplicate class definition")) {
                            // Note the two spaces in error message: "attempted duplicate"
                            continueLoading = true;// this class was already loaded. Keep looping.
                            loadedClasses.put(classname, classBytes);
                        }
                        continue;
                    }
                    throw new IOException(t);
                }
            }
            unloadedClasses.removeAll(loadedClasses.keySet());
        }
    }

    private static boolean isTransformingCurrentClass(String className, String superName, Set<String> interfaceNamesSet, Map<String, byte[]> classBytesMap) {
        // Check to see if any of our utility classes are the same as the class being loaded currently.
        // If not, we want to use the default defineClass() mechanism below
        for (byte[] classBytes : classBytesMap.values()) {
            ClassNode result = WeaveUtils.convertToClassNode(classBytes);
            if (className.equals(result.name) || superName.equals(result.name) || interfaceNamesSet.contains(result.name) ||
                    className.equals(result.superName) || superName.equals(result.superName)) {
                return true;
            }
        }

        return false;
    }

    private NewClassAppender() {
    }
}
