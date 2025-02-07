/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.text.MessageFormat;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

class JVMAgentOtelClassLoader extends URLClassLoader {
    // multi release jars were added in java 9
    private static final int MIN_MULTI_RELEASE_JAR_JAVA_VERSION = 9;
    // current java version
    // TODO we might have this info someplace else
    private static final int JAVA_VERSION;

    static {
        String javaSpecVersion = System.getProperty("java.specification.version");
        if ("1.8".equals(javaSpecVersion)) {
            JAVA_VERSION = 8;
        } else {
            JAVA_VERSION = Integer.parseInt(javaSpecVersion);
        }
    }

    private static final boolean MULTI_RELEASE_JAR_ENABLE =
            JAVA_VERSION >= MIN_MULTI_RELEASE_JAR_JAVA_VERSION;
    private static final String META_INF = "META-INF/";
    private static final String META_INF_VERSIONS = META_INF + "versions/";
    private Manifest manifest; // TODO make this final
    private CodeSource codeSource; // TODO make this final
    // TODO adding the otel jar file to this class loader, might have 2 different class loaders, one for otel, one for non otel
    private JarFile otelJarFile;
    private final String jarEntryPrefix;

    static {
        try {
            registerAsParallelCapable();
        } catch (Throwable t) {
            System.err.println(MessageFormat.format("Unable to register as parallel-capable: {0}", t));
        }
    }

    public JVMAgentOtelClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        try {
            // TODO this try block here is bad. Need to move the getJarFileInAgent call to before this classloader is instantiated
            File javaagentFile = EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(BootstrapLoader.OTEL_JAR_NAME);
            otelJarFile = new JarFile(javaagentFile, false);
            codeSource = new CodeSource(javaagentFile.toURI().toURL(), (Certificate[]) null);
            manifest = otelJarFile.getManifest();

        } catch (IOException e) {
            otelJarFile = null;
            // need to properly treat this, but this should never happen
        }

        jarEntryPrefix = "inst/";

    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // ContextStorageOverride is meant for library instrumentation we don't want it to apply to our
        // bundled grpc
        if ("io.grpc.override.ContextStorageOverride".equals(name)) {
            throw new ClassNotFoundException(name);
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            // first search agent classes
            if (clazz == null) {
                clazz = findAgentClass(name);
            }
            // search from parent and urls added to this loader
            if (clazz == null) {
                clazz = super.loadClass(name, false);
            }
            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }
    }

    private Class<?> findAgentClass(String name) throws ClassNotFoundException {
        JarEntry jarEntry = findJarEntry(name.replace('.', '/') + ".class");
        if (jarEntry != null) {
            byte[] bytes;
            try {
                bytes = getJarEntryBytes(jarEntry);
            } catch (IOException exception) {
                throw new ClassNotFoundException(name, exception);
            }

            definePackageIfNeeded(name);
            return defineClass(name, bytes);
        }

        return null;
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length, codeSource);
    }

    private JarEntry findJarEntry(String name) {
        // shading renames .class to .classdata
        boolean isClass = name.endsWith(".class");
        if (isClass) {
            name += getClassSuffix();
        }
        JarEntry jarEntry = otelJarFile.getJarEntry(jarEntryPrefix + name);
        if (MULTI_RELEASE_JAR_ENABLE) {
            jarEntry = findVersionedJarEntry(jarEntry, name);
        }
        return jarEntry;
    }

    private JarEntry findVersionedJarEntry(JarEntry jarEntry, String name) {
        // same logic as in JarFile.getVersionedEntry
        if (!name.startsWith(META_INF)) {
            // search for versioned entry by looping over possible versions form high to low
            int version = JAVA_VERSION;
            while (version >= MIN_MULTI_RELEASE_JAR_JAVA_VERSION) {
                JarEntry versionedJarEntry =
                        otelJarFile.getJarEntry(jarEntryPrefix + META_INF_VERSIONS + version + "/" + name);
                if (versionedJarEntry != null) {
                    return versionedJarEntry;
                }
                version--;
            }
        }

        return jarEntry;
    }

    // suffix appended to class resource names
    // this is in a protected method so that unit tests could override it
    protected String getClassSuffix() {
        return "data";
    }

    private byte[] getJarEntryBytes(JarEntry jarEntry) throws IOException {
        int size = (int) jarEntry.getSize();
        byte[] buffer = new byte[size];
        try (InputStream is = otelJarFile.getInputStream(jarEntry)) {
            int offset = 0;
            int read;

            while (offset < size && (read = is.read(buffer, offset, size - offset)) != -1) {
                offset += read;
            }
        }

        return buffer;
    }

    private void definePackageIfNeeded(String className) {
        String packageName = getPackageName(className);
        if (packageName == null) {
            return;
        }
        if (getPackage(packageName) == null) {
            try {
                definePackage(packageName, manifest, codeSource.getLocation());
            } catch (IllegalArgumentException exception) {
                if (getPackage(packageName) == null) {
                    throw new IllegalStateException("Failed to define package", exception);
                }
            }
        }
    }

    private static String getPackageName(String className) {
        int index = className.lastIndexOf('.');
        return index == -1 ? null : className.substring(0, index);
    }
}
