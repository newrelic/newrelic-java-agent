/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureClassLoader;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.weave.utils.Streams;

/**
 * Sometimes we need to implement interfaces that are not available to our classloader. This classloader loads classes
 * from the newrelic jar using another classloader as the parent classloader.
 */
public class CleverClassLoader extends SecureClassLoader {
    public CleverClassLoader(ClassLoader parent) {
        super(parent);
    }

    @SuppressWarnings("unchecked")
    public Class loadClassSpecial(String name) throws ClassNotFoundException, IOException {
        String fileName = name.replace('.', '/');
        fileName += ".class";

        InputStream inStream = AgentBridge.getAgent().getClass().getClassLoader().getResourceAsStream(fileName);
        if (inStream == null) {
            throw new ClassNotFoundException("Unable to find class " + name);
        }
        try {
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            Streams.copy(inStream, oStream);
            return loadClass(name, oStream.toByteArray());
        } finally {
            inStream.close();
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("com.newrelic")) {
            try {
                return AgentBridge.getAgent().getClass().getClassLoader().loadClass(name);
            } catch (NoClassDefFoundError e) {
                try {
                    return loadClassSpecial(name);
                } catch (IOException e1) {
                    throw e;
                }
            }
        } else {
            return super.loadClass(name);
        }
    }

    @SuppressWarnings("unchecked")
    protected Class loadClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
