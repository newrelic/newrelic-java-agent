/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.third.party.resources.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This loader only delegates on "java." classes. Used for testing.
 */
public class NoParentURLClassloader extends URLClassLoader {

    public NoParentURLClassloader(URL[] urls) {
        super(urls, null);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("java.")) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (c == null) {
                    // If still not found, then invoke findClass
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

}
