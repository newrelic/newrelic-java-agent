/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

public class ManifestUtils {

    /**
     * Attempt to figure out the version of a given library/jar from its MANIFEST.MF file.
     * If nothing can be found, use the default value provided as a fallback.
     * 
     * This first attempts to get the manifest file via a "fast path" using the jar of
     * the provided class file. If that fails, we scan all manifests for the correct one
     * based on the expected jar name.
     * 
     * @param clazz a class to get the jar for
     * @param jarName the name of the jar file to do a "contains" match on
     * @param defaultVersion a default value to return if nothing is found in the manifest
     * @return the version of the library based on the MANIFEST.MF file
     */
    public static String getVersionFromManifest(Class<?> clazz, String jarName, String defaultVersion) {
        return getVersionFromManifest(clazz, jarName, defaultVersion, false);
    }

    /**
     * For testing only
     */
    static String getVersionFromManifest(Class<?> clazz, String jarName, String defaultVersion, boolean forceFallback) {
        String version = null;
        try {
            // Ugh, I really don't like this but I'm not sure how else to force testing of the fallback code
            if (forceFallback) {
                throw new RuntimeException("Forced exception");
            }

            URL jarForClass = clazz.getProtectionDomain().getCodeSource().getLocation();
            JarFile jarFile = new JarFile(jarForClass.getPath());
            version = getVersionFromManifest(jarFile.getManifest());
        } catch (Exception e) {
            try {
                // Fallback to searching through all MANIFEST.MF files
                Enumeration<URL> manifests = clazz.getClassLoader().getResources(JarFile.MANIFEST_NAME);
                while (manifests.hasMoreElements()) {
                    URL url = manifests.nextElement();
                    if (url.getFile().contains(jarName)) {

                        try (InputStream manifestStream = url.openStream()) {
                            Manifest manifest = new Manifest(manifestStream);
                            version = getVersionFromManifest(manifest);
                            break;
                        }
                    }
                }
            } catch (Exception e2) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, "Unable to determine version from manifest: " +
                        e.toString());
            }
        }

        return version == null ? defaultVersion : version;
    }

    public static String getVersionFromManifest(Manifest manifest) {
        String version = getVersion(manifest.getMainAttributes());
        if (version == null && !manifest.getEntries().isEmpty()) {
            version = getVersion(manifest.getEntries().values().iterator().next());
        }

        return version;
    }

    private static String getVersion(Attributes attributes) {
        String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        if (version == null) {
            version = attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
            if (version == null) {
                // lots of jars specify their version in the Bundle-Version. I believe it's an OSGi thing
                version = attributes.getValue("Bundle-Version");
            }
            if (version == null) {
                // some JDBC drivers specify their version in Driver-Version
                version = attributes.getValue("Driver-Version");
            }
        }
        return version;
    }
}
