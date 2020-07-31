/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.weavepackage.WeavePackageConfig;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;

/**
 * Attempts to open jars and obtain version information from manifests.
 *
 * @since Nov 20, 2012
 */
class JarCollectorServiceProcessor {

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

    /**
     * The max number of jars which we allow to be read in.
     */
    private static final int MAX_MAP_SIZE = 1000;

    private static final JarInfo NON_JAR = new JarInfo(null, null);
    private static final JarInfo JAR_ERROR = new JarInfo(null, null);
    private static final List<String> ATTRIBUTES_TO_COLLECT =
            ImmutableList.of(Attributes.Name.IMPLEMENTATION_VENDOR.toString(), Attributes.Name.IMPLEMENTATION_VENDOR_ID.toString());
    private final boolean skipTempJars; // default true

    /**
     * The list of jars to ignore.
     */
    private final List<String> ignoreJars;

    /**
     * The set of jar urls that have been sent to the collector.
     */
    private final Map<URL, JarInfo> sentJars;

    /**
     * Creates this JarCollectorServiceProcessor.
     */
    public JarCollectorServiceProcessor() {
        this(ServiceFactory.getConfigService().getDefaultAgentConfig().getIgnoreJars());
    }

    /**
     * Creates this JarCollectorServiceProcessor.
     */
    JarCollectorServiceProcessor(List<String> ignoreJars) {
        super();
        this.ignoreJars = ignoreJars;
        sentJars = CacheBuilder.newBuilder().maximumSize(MAX_MAP_SIZE).weakKeys().<URL, JarInfo>build().asMap();
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        skipTempJars = config.getValue("jar_collector.skip_temp_jars", true);
        if (!skipTempJars) {
            Agent.LOG.finest("Jar collector: temporary jars will be transmitted to the host");
        }
    }

    /**
     * Grabs the jars and version from the input information.
     *
     * @param urlsToProcess Jars to be processed.
     * @param sendAll True if all jars should be returned.
     * @return The jars to be sent to the collector. Typically this will only be the new jars unless returnAllJars is
     * true.
     */
    protected synchronized List<Jar> processModuleData(Collection<URL> urlsToProcess, boolean sendAll) {

        urlsToProcess = new HashSet<>(urlsToProcess);
        List<Jar> jars = new ArrayList<>();

        if (sendAll) {
            urlsToProcess.addAll(sentJars.keySet());
        } else {
            urlsToProcess.removeAll(new HashSet<>(sentJars.keySet()));
        }

        Map<URL, JarInfo> processedUrls = processUrls(urlsToProcess, jars);
        sentJars.putAll(processedUrls);

        return jars;
    }

    /**
     * Processes the urls in the input array.
     *
     * @param urls Urls to be processed.
     * @param jars Where new jars should be added.
     */
    private Map<URL, JarInfo> processUrls(final Collection<URL> urls, final List<Jar> jars) {
        Map<URL, JarInfo> jarDetails = new HashMap<>();

        for (URL address : urls) {
            JarInfo jar = NON_JAR;

            try {
                if (skipTempJars && isTempFile(address)) {
                    Agent.LOG.log(Level.FINE, "Skipping temp jar file {0}", address.toString());
                } else {
                    Agent.LOG.log(Level.FINEST, "Processing jar file {0}", address.toString());
                    jar = processUrl(address, jars);
                }
            } catch (Exception e) { // must catch Exception here - bad URIs cause IllegalArgument, etc.
                Agent.LOG.log(Level.FINEST, "While processing {0}: {1}: {2}", address, e.getClass().getSimpleName(),
                        e.getMessage());
            }

            jarDetails.put(address, jar);
        }
        return jarDetails;
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

    static boolean isTempFile(File file) {
        file = file.getParentFile();
        if (null == file) {
            return false;
        } else {
            if (TEMP_DIRECTORY.equals(file)) {
                return true;
            }
            return isTempFile(file);
        }
    }

    /**
     * Process a jar.
     */
    private JarInfo processUrl(final URL url, final List<Jar> jars) {
        try {
            if (!url.getFile().endsWith(JAR_EXTENSION)) {
                return NON_JAR;
            }
            Agent.LOG.log(Level.FINEST, "URL has file path {0}.", url.getFile());
            return handleJar(url, jars);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "Error processing the file path : {0}", e.getMessage());
            return JAR_ERROR;
        }
    }

    /**
     * Handles a jar file.
     *
     * @param url The full path to the jar.
     * @param jars Where new jars should be added.
     */
    private JarInfo handleJar(final URL url, final List<Jar> jars) {
        JarInfo jarInfo = getJarInfoSafe(url);

        addJarAndVersion(url, jarInfo, jars);
        return jarInfo;
    }

    static JarInfo getJarInfoSafe(URL url) {
        Map<String, String> attributes = new HashMap<>();
        try {
            String sha1Checksum = ShaChecksums.computeSha(url);
            attributes.put(SHA1_CHECKSUM_KEY, sha1Checksum);
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINE, "Error getting jar file sha1 checksum : {0}", ex.getMessage());
            Agent.LOG.log(Level.FINEST, ex, "{0}", ex.getMessage());
            attributes.put("sha1error", getErrorMessage(ex));
        }

        try {
            String sha512Checksum = ShaChecksums.computeSha512(url);
            attributes.put(SHA512_CHECKSUM_KEY, sha512Checksum);
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINE, "Error getting jar file sha512 checksum : {0}", ex.getMessage());
            Agent.LOG.log(Level.FINEST, ex, "{0}", ex.getMessage());
            attributes.put("sha512error", getErrorMessage(ex));
        }

        JarInfo jarInfo;
        try {
            jarInfo = getJarInfo(url, attributes);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, "Trouble getting version from {0} jar. Adding jar without version.",
                    url.getFile());
            jarInfo = new JarInfo(UNKNOWN_VERSION, attributes);
        }

        return jarInfo;
    }

    private static String getErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (null == message) {
            message = ex.toString();
        }
        return ex.getClass().getName() + " : " + message;
    }

    private static JarInfo getJarInfo(URL url, Map<String, String> attributes) throws IOException {

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
                Agent.LOG.log(Level.FINEST, ex, "{0}", ex.getMessage());
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
     * @param jars Where new jars should be added.
     */
    boolean addJarAndVersion(URL url, JarInfo jarInfo, final List<Jar> jars) {
        if (jarInfo == null) {
            jarInfo = JarInfo.MISSING;
        }

        // do not add if it is a jar we should ignore
        boolean added = false;

        String jarFile = null;
        try {
            jarFile = parseJarName(url);
            if (shouldAttemptAdd(jarFile)) {
                jars.add(new Jar(jarFile, jarInfo));
                added = true;
            }

        } catch (URISyntaxException e) {
            Agent.LOG.log(Level.FINEST, e, "{0}", e.getMessage());
        }

        // perform the logging
        if (added) {
            Agent.LOG.log(Level.FINER, "Adding the jar {0} with version {1}.", jarFile, jarInfo.version);
        } else {
            Agent.LOG.log(Level.FINER, "Not taking version {0} for jar {1}.", jarInfo.version, jarFile);
        }
        return added;
    }

    /**
     * Removes the full package from the jar name and also gets rid of spaces. This only needs to be called from
     * addJarAndVersion.
     *
     * @param url The name of the jar. This can be full path.
     * @return The name of the jar to put in the internal map.
     */
    static String parseJarName(final URL url) throws URISyntaxException {
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.toURI());
            return file.getName().trim();
        } else {
            Agent.LOG.log(Level.FINEST, "Parsing jar file name from {0}", url);
            String path = url.getFile();
            int end = path.lastIndexOf(JAR_EXTENSION);
            if (end > 0) {
                path = path.substring(0, end);
                int start = path.lastIndexOf(File.separator);
                if (start > -1) {
                    return path.substring(start + 1) + JAR_EXTENSION;
                } else {
                    return path + JAR_EXTENSION;
                }
            }
            throw new URISyntaxException(url.getPath(), "Unable to parse the jar file name from a URL");
        }
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

    static Collection<Jar> getWeaveJars(Map<File, WeavePackageConfig> weaveConfigurations) {

        Collection<Jar> jars = new ArrayList<>();

        for (Entry<File, WeavePackageConfig> entry : weaveConfigurations.entrySet()) {

            String sha1Checksum = "UNKNOWN";
            try {
                sha1Checksum = ShaChecksums.computeSha(entry.getKey());
            } catch (Exception ex) {
                Agent.LOG.log(Level.FINE, "Error getting weave file checksum : {0}", ex.getMessage());
                Agent.LOG.log(Level.FINEST, ex, "{0}", ex.getMessage());
            }

            Map<String, String> attributes = ImmutableMap.of(
                    "weaveFile", entry.getValue().getSource(),
                    JarCollectorServiceProcessor.SHA1_CHECKSUM_KEY, sha1Checksum);

            JarInfo info = new JarInfo(Float.toString(entry.getValue().getVersion()), attributes);
            Jar jar = new Jar(entry.getValue().getName(), info);
            jars.add(jar);
        }

        return jars;
    }

}
