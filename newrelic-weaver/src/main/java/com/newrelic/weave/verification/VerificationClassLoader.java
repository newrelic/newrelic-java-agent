/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.verification;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A partially-delegating classloader used to verify weaved instrumentation.
 */
public class VerificationClassLoader extends URLClassLoader {

    VerificationClassLoader(URL[] urls) {
        // Use the system classloader as our parent
        super(urls);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findClass(name);
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public URL getResource(String name) {
        // Check our URLs first. This allows us to verify instrumentation modules
        // that use a different version of java (the different java runtime jars is one of our known URLS)
        URL resource = super.findResource(name);
        if (resource != null) {
            return resource;
        }

        // Let ClassLoader check our parent (system classloader).
        return super.getResource(name);
    }
}
