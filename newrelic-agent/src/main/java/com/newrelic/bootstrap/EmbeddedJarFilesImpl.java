/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.bootstrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.logging.IAgentLogger;

public class EmbeddedJarFilesImpl implements EmbeddedJarFiles {

    private static final String[] INTERNAL_JAR_FILE_NAMES = new String[] { BootstrapLoader.AGENT_BRIDGE_JAR_NAME,
            BootstrapLoader.API_JAR_NAME, BootstrapLoader.WEAVER_API_JAR_NAME,  BootstrapLoader.NEWRELIC_SECURITY_AGENT};

    public static final EmbeddedJarFiles INSTANCE = new EmbeddedJarFilesImpl();

    // This configures how old a temporary agent jar file must be before it will be automatically deleted
    // on startup (in hours, whole numbers only). If this value is empty/null, then stale jar files
    // will not be checked for or deleted. By default, this feature is disabled.
    private static final String TEMP_JAR_FILE_AGE_THRESHOLD_ENV_CONFIG = "NEW_RELIC_TEMP_JARFILE_AGE_THRESHOLD_HOURS";
    private static final String TEMP_JAR_FILE_AGE_THRESHOLD_SYSPROP_CONFIG = "newrelic.config.temp_jarfile_age_threshold_hours";

    private static final String JAVA_IO_TMP_DIR = "java.io.tmpdir";

    /**
     * A map of jar names to the temp files containing those jars.
     */
    private final Function<String, File> embeddedAgentJarFiles = AgentBridge.collectionFactory.createLoadingCache(this::loadJarFile);

    /**
     * Cleanup any stale temporary agent jar files in the defined temp folder if a threshold has
     * been configured via the NEW_RELIC_TEMP_JARFILE_AGE_THRESHOLD_HOURS env variable or the
     * newrelic.config.temp_jarfile_age_threshold_hours system property.
     * <p>
     * A file is eligible to be deleted if it starts with one of the internal
     * agent jar prefixes ("agent-bridge", "newrelic-api", etc.), has the ".jar"
     * extension and is older than the specified cutoff value.
     */
    public static void cleanupStaleTempJarFiles() {
        int thresholdInHours = getStaleTempJarFileAgeConfig();

        if (thresholdInHours > 0) {
            IAgentLogger logger = AgentLogManager.getLogger();

            // Add other prefixes to the list of file prefixes to clean up
            List<String> internalJarNamePrefixes = new ArrayList<>(Arrays.asList(INTERNAL_JAR_FILE_NAMES));
            internalJarNamePrefixes.addAll(Arrays.asList("instrumentation", "newrelic-bootstrap", "newrelic-security-api", "agent-bridge-datastore"));

            File tmpDir = BootstrapLoader.getTempDir();
            if (tmpDir == null) {
                tmpDir = new File(System.getProperty(JAVA_IO_TMP_DIR));
            }

            if (!tmpDir.exists() || !tmpDir.isDirectory()) {
                return;
            }

            long cutoffTime = System.currentTimeMillis() - (thresholdInHours * 60L * 60L * 1000L);
            File[] files = tmpDir.listFiles((dir, name) -> {
                if (!name.endsWith(".jar")) {
                    return false;
                }
                for (String jarName : internalJarNamePrefixes) {
                    if (name.startsWith(jarName)) {
                        return true;
                    }
                }
                return false;
            });
            if (files == null) {
                return;
            }


            logger.info("New Relic Agent: Removing stale temporary agent file jars from " + tmpDir.getAbsoluteFile() + " older than " + thresholdInHours + " hour(s)");

            int deletedCount = 0;
            long totalBytes = 0;
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    try {
                        long fileSize = file.length();
                        if (file.delete()) {
                            deletedCount++;
                            totalBytes += fileSize;
                        }
                    } catch (SecurityException ignored) {
                        // Unable to delete, silently continue
                    }
                }
            }

            logger.info("New Relic Agent: Deleted " + deletedCount + " stale temporary jar files freeing up " + totalBytes + " bytes");
        }
    }

    /**
     * Return the configured threshold for deleting stale agent temp jars in the temp folder.
     * The value must be a whole number. If not set, return 0 which signals to the agent
     * not to try and remove stale jar files.
     *
     * @return the configured threshold value or 0 if not set
     */
    private static int getStaleTempJarFileAgeConfig() {
        String sysVal = System.getProperty(TEMP_JAR_FILE_AGE_THRESHOLD_SYSPROP_CONFIG);
        String envVal = System.getenv(TEMP_JAR_FILE_AGE_THRESHOLD_ENV_CONFIG);
        int threshold = 0;

        try {
            if (envVal != null) {
                threshold = Integer.parseInt(envVal);
            } else if (sysVal != null) {
                threshold = Integer.parseInt(sysVal);
            }
        } catch (NumberFormatException ignored) {
        }

        return threshold;
    }

    private final String[] jarFileNames;

    public EmbeddedJarFilesImpl() {
        this(INTERNAL_JAR_FILE_NAMES);
    }

    public EmbeddedJarFilesImpl(String[] jarFileNames) {
        super();
        this.jarFileNames = jarFileNames;
    }

    @Override
    public File getJarFileInAgent(String jarNameWithoutExtension) throws IOException {
        try {
            return embeddedAgentJarFiles.apply(jarNameWithoutExtension);
        } catch (UncheckedIOException e) {
            // Unwrap and rethrow as checked IOException
            throw e.getCause();
        }
    }

    @Override
    public String[] getEmbeddedAgentJarFileNames() {
        return jarFileNames;
    }

    /**
     * Extracts an embedded JAR file to a temp file.
     */
    private File loadJarFile(String jarNameWithoutExtension) {
        try {
            InputStream jarStream = EmbeddedJarFilesImpl.class.getClassLoader()
                    .getResourceAsStream(jarNameWithoutExtension + ".jar");

            if (jarStream == null) {
                throw new FileNotFoundException(jarNameWithoutExtension + ".jar");
            }

            File file = File.createTempFile(jarNameWithoutExtension, ".jar",
                    BootstrapLoader.getTempDir());
            file.deleteOnExit();

            try (OutputStream out = new FileOutputStream(file)) {
                BootstrapLoader.copy(jarStream, out, 8096, true);
                return file;
            }
        } catch (IOException e) {
            // Wrap checked exception since Function can't throw checked exceptions
            throw new UncheckedIOException(e);
        }
    }
}
