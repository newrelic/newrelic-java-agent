/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.third.party.resources.classloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Post-delegating classloader for specific classes
 */
public class PostDelegatingClassLoader extends URLClassLoader {
    private Set<String> classesToLoad = ImmutableSet.of(
            "com.fake.third.party.resources.classloader.MockHttpServletRequest",
            "com.fake.third.party.resources.classloader.MockHttpServletResponse");

    public PostDelegatingClassLoader(URL[] urls) {
        super(urls);
    }

    public void addFile(String path) throws MalformedURLException {
        String urlPath = "jar:file://" + path + "!/";
        addURL(new URL(urlPath));
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = null;
            c = findLoadedClass(name);
            if (null == c) {
                // First, check if the class has already been loaded
                try {

                    if (name.startsWith("javax.servlet.")
                            || name.startsWith("com.newrelic.agent.instrumentation.context.")) {
                        c = findClass(name);
                    } else {
                        // to find the class with this classloader
                        if (classesToLoad.contains(name)) {
                            c = findClass(name);
                        }
                    }
                } catch (ClassNotFoundException e) {
                }
            }
            if (null == c) {
                if (c == null) {
                    c = super.loadClass(name, false);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
