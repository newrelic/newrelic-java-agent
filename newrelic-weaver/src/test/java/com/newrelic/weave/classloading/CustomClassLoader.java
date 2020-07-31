/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.classloading;

import java.net.URL;
import java.net.URLClassLoader;

public class CustomClassLoader extends URLClassLoader {
    private static final ClassLoader BOOTSTRAP = new ClassLoader(null) {
    };

    /**
     * A strategy for handling class or resource loading.
     * <br/>Note: All these strategies will delegate classloading to the bootstrap.
     */
    public enum Strategy {
        // @formatter:off
    	/**Normal java delegation. Check parent classloader first*/
        PARENT_FIRST,
        /**Return null when asked for resources even if they are resolvable*/
        HIDDEN,
        /**Check and load self before delegating to parent*/
        SELF_FIRST,
        // @formatter:on
    }

    private final Strategy classLoadingStrategy;
    private final Strategy resourceLoadingStrategy;

    public CustomClassLoader(Strategy classLoadingStrategy, Strategy resourceLoadingStrategy, URL[] classpath,
            ClassLoader parent) {
        super(classpath, parent);
        this.classLoadingStrategy = classLoadingStrategy;
        this.resourceLoadingStrategy = resourceLoadingStrategy;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            try {
                Class<?> c = BOOTSTRAP.loadClass(name);
                if (null != c) {
                    return c;
                }
            } catch (Throwable t) {
            }
            switch (classLoadingStrategy) {
            case SELF_FIRST:
                try {
                    return loadWithoutDelegation(name, resolve);
                } catch(ClassNotFoundException cnfe) {
                    return super.loadClass(name, resolve);
                }
            case HIDDEN:
                // loadClass behaves normally
            case PARENT_FIRST:
                return super.loadClass(name, resolve);
            default:
                throw new RuntimeException("Invalid strategy");
            }
        }
    }

    /**
     * Attempt to load the class without delegating to any other classloaders.
     */
    private Class<?> loadWithoutDelegation(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = null;
        c = findLoadedClass(name);
        if (null == c) {
            // First, check if the class has already been loaded
            try {
                c = findClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        if (null == c) {
            throw new ClassNotFoundException(name);
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public URL getResource(String name) {
        switch (resourceLoadingStrategy) {
        case HIDDEN:
            return null;
        case SELF_FIRST:
            URL result = findResource(name);
            if(null != result) {
                return result;
            }
            // fall through to parent
        case PARENT_FIRST:
            return super.getResource(name);
        default:
            throw new RuntimeException("Invalid strategy");
        }
    }

    public boolean isPrivateResource(String name) {
        return false;
    }

    @Override
    public String toString() {
        return super.toString() + "classLoadingStrategy:" + classLoadingStrategy + "_resourceLoadingStrategy:"
                + resourceLoadingStrategy;
    }
}
