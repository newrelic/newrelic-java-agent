/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.newrelic.agent.Agent;
import com.newrelic.bootstrap.EmbeddedJarFilesImpl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class ClassResolvers {
    private ClassResolvers() {
    }

    /**
     * Returns a class resolver that can resolve classes from the api jars inside of the agent jar.
     * 
     * @see EmbeddedJarFilesImpl
     */
    public static ClassResolver getEmbeddedJarsClassResolver() {

        Collection<ClassResolver> resolvers = new ArrayList<>();
        for (String name : EmbeddedJarFilesImpl.INSTANCE.getEmbeddedAgentJarFileNames()) {
            try {
                resolvers.add(getJarClassResolver(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(name)));
            } catch (IOException e) {
                Agent.LOG.log(Level.SEVERE, e, "Unable to load {0} : {1}", name, e.getMessage());
            }
        }

        return getMultiResolver(resolvers);
    }

    /**
     * Returns a class resolver that finds classes inside a jar file.
     */
    public static ClassResolver getJarClassResolver(final File jarFile) throws IOException {

        // find the class names in the jar file
        final Set<String> classNames = new HashSet<>();
        try (JarFile jar = new JarFile(jarFile)) {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    classNames.add(jarEntry.getName());
                }
            }
        }

        return new ClassResolver() {

            @Override
            public InputStream getClassResource(String internalClassName) throws IOException {
                String resourceName = internalClassName + ".class";
                if (classNames.contains(resourceName)) {
                    final JarFile jar = new JarFile(jarFile);
                    JarEntry entry = jar.getJarEntry(resourceName);

                    // I'm using this BufferedInputStream to override the close method & close the jar
                    return new BufferedInputStream(jar.getInputStream(entry)) {

                        @Override
                        public void close() throws IOException {
                            super.close();
                            jar.close();
                        }

                    };
                }
                return null;
            }

            @Override
            public String toString() {
                return jarFile.getAbsolutePath();
            }

        };
    }

    /**
     * Returns a class resolver that uses {@link ClassLoader#getResource(String)} to find the class.
     * 
     * @param classLoader
     */
    public static ClassResolver getClassLoaderResolver(final ClassLoader classLoader) {
        return new ClassResolver() {

            @Override
            public InputStream getClassResource(String internalClassName) throws IOException {
                URL resource = Utils.getClassResource(classLoader, internalClassName);
                return resource == null ? null : resource.openStream();
            }
        };

    }

    /**
     * Returns a class resolver that iterates through an array of resolvers to resolve a class.
     * 
     * @param resolvers
     */
    public static ClassResolver getMultiResolver(final ClassResolver... resolvers) {
        return new ClassResolver() {

            @Override
            public InputStream getClassResource(String internalClassName) throws IOException {
                for (ClassResolver resolver : resolvers) {
                    InputStream classResource = resolver.getClassResource(internalClassName);
                    if (classResource != null) {
                        return classResource;
                    }
                }
                return null;
            }
        };

    }

    /**
     * Returns a class resolver that iterates through an list of resolvers to resolve a class.
     * 
     * @param resolvers
     */
    public static ClassResolver getMultiResolver(final Collection<ClassResolver> resolvers) {
        return new ClassResolver() {

            @Override
            public InputStream getClassResource(String internalClassName) throws IOException {
                for (ClassResolver resolver : resolvers) {
                    InputStream classResource = resolver.getClassResource(internalClassName);
                    if (classResource != null) {
                        return classResource;
                    }
                }
                return null;
            }
        };

    }
}
