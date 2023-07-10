/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Utility class containing methods for creating temporary JAR files.
 */
public final class JarUtils {
    
    private static final String NEWRELIC_TEMPDIR = "newrelic.tempdir";

    private JarUtils() {
    }

    /**
     * Create a temporary JAR file with the specified prefix and classes.
     * 
     * @param prefix prefix for the temporary jar file
     * @param classes map of class names to class bytes
     * @return temp JAR file that will be deleted on process exit
     * @throws IOException
     */
    public static File createJarFile(String prefix, Map<String, byte[]> classes) throws IOException {
        return createJarFile(prefix, classes, null);
    }

    /**
     * Create a temporary JAR file with the specified prefix, manifest, and classes.
     * 
     * @param prefix prefix for the temporary jar file
     * @param classes map of class names to class bytes
     * @param manifest JAR file's manifest
     * @return temp JAR file that will be deleted on process exit
     * @throws IOException
     */
    public static File createJarFile(String prefix, Map<String, byte[]> classes, Manifest manifest) throws IOException {
        return createJarFile(prefix, classes, manifest, null);
    }

    public static File createJarFile(String prefix, Map<String, byte[]> classes,
            Manifest manifest, Map<String, byte[]> extensions) throws IOException {
        File file = File.createTempFile(prefix, ".jar", getTempDir());
        file.deleteOnExit(); // Doesn't need to be kept after shutdown.

        if (manifest == null) {
            manifest = new Manifest();
        }
        JarOutputStream outStream = new JarOutputStream(new FileOutputStream(file), manifest);
        writeFilesToJarStream(classes, outStream, extensions);
        return file;
    }

    private static void writeFilesToJarStream(Map<String, byte[]> classes, JarOutputStream outStream,
            Map<String, byte[]> extensions)
            throws IOException {

        Map<String, byte[]> resources = new HashMap<>();
        for (Entry<String, byte[]> entry : classes.entrySet()) {
            resources.put(entry.getKey().replace('.', '/') + ".class", entry.getValue());
        }
        try {
            addJarEntries(outStream, resources);
            addExtensions(outStream, extensions);
        } finally {
            try {
                outStream.close();
            } catch (IOException ioe) {
            }
        }

    }

    private static void addJarEntries(JarOutputStream jarStream, Map<String, byte[]> files) throws IOException {
        for (Entry<String, byte[]> entry : files.entrySet()) {
            JarEntry jarEntry = new JarEntry(entry.getKey());
            jarStream.putNextEntry(jarEntry);
            jarStream.write(entry.getValue());
            jarStream.closeEntry();
        }
    }

    private static void addExtensions(JarOutputStream jarStream, Map<String, byte[]> extensions) throws IOException {
        if (extensions == null) return;

        JarEntry extensionsEntry = new JarEntry("META-INF/extensions/extensions");
        jarStream.putNextEntry(extensionsEntry);
        for (Entry<String, byte[]> entry : extensions.entrySet()) {
            jarStream.write(entry.getKey().getBytes());
            jarStream.write("\r\n".getBytes());
        }
        jarStream.closeEntry();

        for (Entry<String, byte[]> entry : extensions.entrySet()) {
            JarEntry jarEntry = new JarEntry("META-INF/extensions/"+entry.getKey());
            jarStream.putNextEntry(jarEntry);
            jarStream.write(entry.getValue());
            jarStream.closeEntry();
        }
    }

    /**
     * Returns the tempdir that the agent should use, or null for the default tempdir.
     */
    private static File getTempDir() {
        String tempDir = System.getProperty(NEWRELIC_TEMPDIR);
        if (null != tempDir) {
            File tempDirFile = new File(tempDir);
            // If the tempdir doesn't exist other agent code will complain about it 
            if (tempDirFile.exists()) {
                return tempDirFile;
            }
        }
        return null;
    }

}
