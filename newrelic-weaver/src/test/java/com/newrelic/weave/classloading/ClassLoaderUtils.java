/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.classloading;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;

import org.objectweb.asm.tree.ClassNode;

import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassClassFinder;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.NewClassAppender;

public class ClassLoaderUtils {
    static final Method findLoadedClassMethod;
    static {
        try {
            findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoadedClassMethod.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Run the given callable on the given classloader.<br/>
     * The callable class itself will be injected and run on the classloader.
     * This means that all references in the callable must be resolvable by the classloader.
     * The callable cannot be an anonymous inner class.
     */
    @SuppressWarnings("unchecked")
        public static <T> T runOnClassLoader(ClassLoader classloader, Callable<T> callable) {
        try {
            // first make sure the classloader can properly see the Callable interface
            Class<?> clCallable = classloader.loadClass(Callable.class.getName());
            if(Callable.class != clCallable) {
                throw new RuntimeException("Classloader must be able delegate loading of Callable to bootstrap loader.");
            }
            // now inject the impl into the ClassLoader
            URL resource = new ClassClassFinder(callable.getClass()).findResource(callable.getClass().getName());
            byte[] callableImplBytes = Streams.read(resource.openStream(), true);
            ProtectionDomain pd = callable.getClass().getProtectionDomain();
            Class<?> clCallableImpl = NewClassAppender.defineClass(classloader, callable.getClass().getName(),
                    callableImplBytes, 0, callableImplBytes.length, pd);
            if(null == clCallableImpl){
                throw new RuntimeException("Unable to inject callable.");
            }
            Callable<T> runner = (Callable<T>) clCallableImpl.newInstance();
            return runner.call();
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static boolean isClassLoadedOnClassLoader(final ClassLoader classloader, final String classname){
        try {
            return null != findLoadedClassMethod.invoke(classloader, classname);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns bytes for an empty class of the given internalClassName
     */
    public static byte[] generateEmptyClass(String internalClassName) throws IOException {
        byte[] original = WeaveTestUtils.getClassBytes(Template.class.getName());
        ClassNode node = WeaveUtils.convertToClassNode(original);
        node.name = internalClassName;
        return WeaveUtils.convertToClassBytes(node, new ClassCache(new ClassLoaderFinder(ClassLoader.getSystemClassLoader())));
    }
    /**
     * An empty class used to auto generate classes
     */
    public static class Template {
    }
}
