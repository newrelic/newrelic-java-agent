/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.weave.utils.ClassFinder;
import com.newrelic.weave.utils.WeaveUtils;

/**
 * The purpose of this ClassFinder instance is to return a URL to the bytes of the given ClassLoader name which 
 * is stored internally in the Agent in the "observedClassLoaders" map. We can use this to get the full ClassLoader
 * hierarchy without having to use getResource or load any classes ourselves.
 */
public class ClassLoaderClassFinder implements ClassFinder {

    private final Map<String, byte[]> observedClassLoaders;

    public ClassLoaderClassFinder(Map<String, byte[]> observedClassLoaders) {
        this.observedClassLoaders = observedClassLoaders;
    }

    @Override
    public URL findResource(String internalName) {
        try {
            final byte[] classLoaderBytes = observedClassLoaders.get(WeaveUtils.getClassInternalName(internalName));
            if (classLoaderBytes != null) {
                return new URL(null, "classloader:" + internalName, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        return new URLConnection(u) {
                            @Override
                            public void connect() throws IOException {
                                // no-op
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                return new ByteArrayInputStream(classLoaderBytes);
                            }
                        };
                    }
                });
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, e, "Unable to load classloader bytes from memory");
        }
        return null;
    }

}
