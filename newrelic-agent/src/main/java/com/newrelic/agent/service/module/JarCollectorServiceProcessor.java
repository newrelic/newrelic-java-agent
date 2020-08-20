/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.google.common.collect.ImmutableList;
import com.newrelic.Function;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;

/**
 * Attempts to open jars and obtain version information from manifests.
 */
public class JarCollectorServiceProcessor implements Function<URL, JarData> {

    static final String SHA1_CHECKSUM_KEY = "sha1Checksum";
    static final String SHA512_CHECKSUM_KEY = "sha512Checksum";

    /**
     * The extension for a jar file.
     */
    static final String JAR_EXTENSION = ".jar";

    static final String JAR_PROTOCOL = "jar";

    /**
     * Constant for unknown versions.
     */
    static final String UNKNOWN_VERSION = " ";

    @SuppressWarnings("deprecation") // Implementation-Vendor-Id is deprecated
    private static final List<String> ATTRIBUTES_TO_COLLECT =
            ImmutableList.of(
                    Attributes.Name.IMPLEMENTATION_VENDOR.toString(),
                    Attributes.Name.IMPLEMENTATION_VENDOR_ID.toString());

    private final boolean skipTempJars; // default true
    private final Logger logger;

    /**
     * The list of jars to ignore.
     */
    private final List<String> ignoreJars;

    public JarCollectorServiceProcessor(Logger logger, AgentConfig agentConfig) {
        this(agentConfig, agentConfig.getIgnoreJars(), logger);
    }

    /**
     * Creates this JarCollectorServiceProcessor.
     */
    JarCollectorServiceProcessor(Config config, List<String> ignoreJars, Logger logger) {
        this.ignoreJars = new ArrayList<>(ignoreJars);
        this.logger = logger;
        this.skipTempJars = config.getValue("jar_collector.skip_temp_jars", true);
        if (!skipTempJars) {
            logger.log(Level.FINEST, "temporary jars will be transmitted to the host");
        }
    }

    @Override
    public JarData apply(URL url) {
        try {
            return tryProcessSingleURL(url);
        } catch (Throwable t) {
            logger.log(Level.FINE, t, "Exception processing jar at {0}", url);
            return null;
        }
    }

    private JarData tryProcessSingleURL(URL url) throws URISyntaxException {
        if (skipTempJars && isTempFile(url)) {
            logger.log(Level.FINE, "{0} Skipping temp jar file", url);
            return null;
        }

        if (!url.getFile().endsWith(JAR_EXTENSION)) {
            logger.log(Level.FINE, "{0} Skipping file with non-jar extension", url);
            return null;
        }

        JarInfo jarInfo = getJarInfoSafe(url);
        return addJarAndVersion(url, jarInfo);
    }

    /**
     * Returns true if the address protocol is "file" and the file resides within the temp directory.
     */
    static boolean isTempFile(URL address) throws URISyntaxException {
        if (!"file".equals(address.getProtocol())) {
            return false;
        }

        return isTempFile(new File(address.toURI()));
    }

    private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

    static boolean isTempFile(File fileParam) {
        File file = fileParam;
        while (file != null) {
            file = file.getParentFile();
            if (TEMP_DIRECTORY.equals(file)) {
                return true;
            }
        }

        return false;
    }

    JarInfo getJarInfoSafe(URL url) {
        Map<String, String> attributes = new HashMap<>();
        try {
            String sha1Checksum = ShaChecksums.computeSha(url);
            attributes.put(SHA1_CHECKSUM_KEY, sha1Checksum);
        } catch (Exception ex) {
            logger.log(Level.FINE, ex, "{0} Error getting jar file sha1 checksum", url);
        }

        try {
            String sha512Checksum = ShaChecksums.computeSha512(url);
            attributes.put(SHA512_CHECKSUM_KEY, sha512Checksum);
        } catch (Exception ex) {
            logger.log(Level.FINE, ex, "{0} Error getting jar file sha512 checksum", url);
        }

        JarInfo jarInfo;
        try {
            jarInfo = getJarInfo(url, attributes);
        } catch (Exception e) {
            logger.log(Level.FINE, e, "{0} Trouble getting version from jar. Adding jar without version.", url);
            jarInfo = new JarInfo(UNKNOWN_VERSION, attributes);
        }

        return jarInfo;
    }

    private JarInfo getJarInfo(URL url, Map<String, String> attributes) throws IOException {
        try (JarInputStream jarFile = EmbeddedJars.getJarInputStream(url)) {
            try {
                getExtraAttributes(jarFile, attributes);

                Map<String, String> pom = getPom(jarFile);

                // if we find exactly one pom, use it
                if (pom != null) {
                    attributes.putAll(pom);
                    return new JarInfo(pom.get("version"), attributes);
                }
            } catch (Exception ex) {
                logger.log(Level.FINEST, ex, "{0} Exception getting extra attributes or pom.", url);
            }

            String version = getVersion(jarFile);
            if (version == null) {
                version = UNKNOWN_VERSION;
            }

            return new JarInfo(version, attributes);
        }
    }

    /**
     * Returns the values from pom.properties if this file is found. If multiple pom.properties files are found, return
     * null.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, String> getPom(JarInputStream jarFile) throws IOException {
        Map<String, String> pom = null;

        for (JarEntry entry = jarFile.getNextJarEntry(); entry != null; entry = jarFile.getNextJarEntry()) {
            if (entry.getName().startsWith("META-INF/maven") && entry.getName().endsWith("pom.properties")) {
                if (pom != null) {
                    // we've found multiple pom files. bail!
                    return null;
                }
                Properties props = new Properties();
                props.load(jarFile);

                pom = (Map) props;
            }
        }

        return pom;
    }

    static void getExtraAttributes(JarInputStream jarFile, Map<String, String> map) {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return;
        }

        Attributes attributes = manifest.getMainAttributes();
        for (String name : ATTRIBUTES_TO_COLLECT) {
            String value = attributes.getValue(name);
            if (null != value) {
                map.put(name, value);
            }
        }
    }

    static String getVersion(JarInputStream jarFile) {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return null;
        }

        return ManifestUtils.getVersionFromManifest(manifest);
    }

    /**
     * Called to add a jar with its version. If version is null then it will be set to UNKNOWN_VERSION.
     *
     * @param url The full path to the jar.
     * @param jarInfo Jar version and attributes.
     */
    JarData addJarAndVersion(URL url, JarInfo jarInfo) throws URISyntaxException {
        if (jarInfo == null) {
            jarInfo = JarInfo.MISSING;
        }

        String jarFile = parseJarName(url);
        if (shouldAttemptAdd(jarFile)) {
            logger.log(Level.FINE, "{0} Adding the file {1} with version {2}.", url, jarFile, jarInfo.version);
            return new JarData(jarFile, jarInfo);
        }

        return null;
    }

    /**
     * Removes the full package from the jar name and also gets rid of spaces. This only needs to be called from
     * addJarAndVersion.
     *
     * @param url The name of the jar. This can be full path.
     * @return The name of the jar to put in the internal map.
     */
    String parseJarName(final URL url) throws URISyntaxException {
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.toURI());
            return file.getName().trim();
        }

        logger.log(Level.FINEST, "{0} Parsing jar file name", url);
        String path = url.getFile();
        int end = path.lastIndexOf(JAR_EXTENSION);
        if (end > 0) {
            path = path.substring(0, end);
            int start = path.lastIndexOf(File.separator);
            if (start > -1) {
                return path.substring(start + 1) + JAR_EXTENSION;
            }

            return path + JAR_EXTENSION;
        }

        throw new URISyntaxException(url.getPath(), "Unable to parse the jar file name from a URL");
    }

    /**
     * Returns true if the jar file should be sent to the collector.
     *
     * @param jarFile The name of the jar file.
     * @return True if the jar file should be added, else false.
     */
    private boolean shouldAttemptAdd(final String jarFile) {
        return !ignoreJars.contains(jarFile);
    }

}
