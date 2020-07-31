/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import static com.newrelic.agent.extension.ExtensionTest.readClass;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.objectweb.asm.Type;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.weave.utils.JarUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class JarExtensionTest {
    
    @BeforeClass
    public static void setup() throws Exception {
        // Can no longer cast AppClassLoader to URLClassLoader in Java 9+, instead iterate through classpath to get URLs
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);
        URL[] urls = new URL[classpathEntries.length];

        try {
            for (int i = 0; i < classpathEntries.length; i++) {
                urls[i] = new File(classpathEntries[i]).toURI().toURL();
            }
        } catch (MalformedURLException ex) {
        }

        URL[] newUrls = Arrays.copyOf(urls, urls.length + 1);

        File extensionJar = createExtensionJar();
        URL u = extensionJar.toURL();
        newUrls[newUrls.length - 1] = u;

        URLClassLoader urlClassLoader = (URLClassLoader) AgentBridge.getAgent().getClass().getClassLoader();
        Class urlClass = URLClassLoader.class;
        Method method = urlClass.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        for (URL url : newUrls) {
            method.invoke(urlClassLoader, url);
        }
    }

    @Test
    public void javaagentExtension() throws Exception {
        Class<?> extensionTestClass = AgentBridge.getAgent().getClass().getClassLoader().loadClass(
                "com.newrelic.agent.extension.ExtensionTest$JavaAgentExtensionTest");
        Callable<Void> extensionTest = (Callable<Void>) extensionTestClass.newInstance();
        extensionTest.call();
    }

    @Test
    public void testNonRetransformPath() throws Exception {
        Class<?> extensionTestClass = AgentBridge.getAgent().getClass().getClassLoader().loadClass(
                "com.newrelic.agent.extension.ExtensionTest$NonRetransformPathTest");
        Callable<Void> extensionTest = (Callable<Void>) extensionTestClass.newInstance();
        extensionTest.call();
    }

    private static File createExtensionJar() throws IOException {
        Manifest manifest = new Manifest();

        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        File file = JarUtils.createJarFile("extensionjar", ImmutableMap.of(
                Type.getInternalName(ExtensionTest.class), readClass(ExtensionTest.class).b),
                manifest);

        return file;
    }
}
