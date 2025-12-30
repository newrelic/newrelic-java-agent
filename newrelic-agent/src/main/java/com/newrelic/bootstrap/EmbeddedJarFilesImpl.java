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
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;

public class EmbeddedJarFilesImpl implements EmbeddedJarFiles {

    private static final String[] INTERNAL_JAR_FILE_NAMES = new String[] { BootstrapLoader.AGENT_BRIDGE_JAR_NAME,
            BootstrapLoader.API_JAR_NAME, BootstrapLoader.WEAVER_API_JAR_NAME,  BootstrapLoader.NEWRELIC_SECURITY_AGENT};

    public static final EmbeddedJarFiles INSTANCE = new EmbeddedJarFilesImpl();
    private static final String NEWRELIC_TEMP_DIR_CLEANUP_DISABLE = "newrelic.tempdir.cleanup.disable";
    private static final String NEWRELIC_TEMP_DIR_CLEANUP_AGE_MS = "newrelic.tempdir.cleanup.age.ms";
    private static final int LAST_24HOURS = 24;
    private static final String JAR = ".jar";
    private static final String JAVA_IO_TMP_DIR = "java.io.tmpdir";

    /**
     * A map of jar names to the temp files containing those jars.
     */
    private final LoadingCache<String, File> embeddedAgentJarFiles = Caffeine.newBuilder().executor(Runnable::run).build(
            new CacheLoader<String, File>() {

                @Override
                public File load(String jarNameWithoutExtension) throws IOException {
                    InputStream jarStream = EmbeddedJarFilesImpl.class.getClassLoader().getResourceAsStream(
                            jarNameWithoutExtension + ".jar");
                    if (jarStream == null) {
                        throw new FileNotFoundException(jarNameWithoutExtension + ".jar");
                    }

                    File file = File.createTempFile(jarNameWithoutExtension, ".jar", BootstrapLoader.getTempDir());
                    file.deleteOnExit(); // Doesn't need to be kept after shutdown.

                    try (OutputStream out = new FileOutputStream(file)) {
                        BootstrapLoader.copy(jarStream, out, 8096, true);

                        return file;
                    }

                }

            });

    private final String[] jarFileNames;

    public EmbeddedJarFilesImpl() {
        this(INTERNAL_JAR_FILE_NAMES);
    }

    public EmbeddedJarFilesImpl(String[] jarFileNames) {
        super();
        cleanupOldAgentTempFiles();
        this.jarFileNames = jarFileNames;
    }

    /**
     * Cleans up old agent temp files in the temp directory.
     * These are files created by the agent in previous runs that may not have been deleted
     * due to abnormal termination.
     * The cleanup is skipped if running on a CRaC-enabled JVM to avoid interfering with checkpoint/restore.
     */
    private void cleanupOldAgentTempFiles() {
        try {
            // Allow disable via system property for safety and testing
            String disableProp = System.getProperty(NEWRELIC_TEMP_DIR_CLEANUP_DISABLE, "false");
            if (Boolean.parseBoolean(disableProp)) {
                return;
            }

            // If running on a CRaC-enabled JVM (or test marker is present), avoid cleaning up temp files here.
            // Use the helper to detect CRaC or the test marker class.
            // Allow tests to force cleanup even if CRaC is detected
            boolean forceCleanup = Boolean.parseBoolean(System.getProperty("newrelic.tempdir.cleanup.force", "false"));
            if (!forceCleanup && isCracPresent()) {
                return;
            }

            // 24 hours default
            long ageMs = TimeUnit.HOURS.toMillis(LAST_24HOURS);
            try {
                String ageProp = System.getProperty(NEWRELIC_TEMP_DIR_CLEANUP_AGE_MS);
                if (ageProp != null && !ageProp.isEmpty()) {
                    ageMs = Long.parseLong(ageProp);
                }
            } catch (NumberFormatException nfe) {
                // ignore and keep default
            }

            File tmpDir = BootstrapLoader.getTempDir();
            if (tmpDir == null) {
                tmpDir = new File(System.getProperty(JAVA_IO_TMP_DIR));
            }
            if (!tmpDir.isDirectory()) {
                return;
            }

            final long cutoff = System.currentTimeMillis() - ageMs;

             for (String base : INTERNAL_JAR_FILE_NAMES) {
                 File[] matches = tmpDir.listFiles((dir, name) -> name.startsWith(base) && name.endsWith(JAR));
                if (matches == null) continue;

                for (File f : matches) {
                     try {
                         long lastMod = f.lastModified();
                         if (lastMod > 0 && lastMod < cutoff) {
                             if (f.delete()) {
                                 System.err.println("Deleted leftover agent temp file: " + f.getAbsolutePath());
                             } else {
                                 System.err.println("Failed to delete leftover agent temp file: " + f.getAbsolutePath());
                             }
                         }
                     } catch (Throwable t) {
                         System.err.println("Error while attempting to delete leftover agent temp file '" + f + "': " + t);
                     }
                 }
             }
        } catch (Throwable t) {
            System.err.println("Failed to cleanup old agent temp files: " + t);
        }
    }

    @Override
    public File getJarFileInAgent(String jarNameWithoutExtension) throws IOException {
        return embeddedAgentJarFiles.get(jarNameWithoutExtension);
    }

    @Override
    public String[] getEmbeddedAgentJarFileNames() {
        return jarFileNames;
    }

    // Detect presence of a CRaC marker (test-friendly) in a robust way.
    // Checks a test override system property, then tries to load the
    // marker class across several classloaders, and falls back to a resource lookup.
    private boolean isCracPresent() {
        // Test-only override to simulate CRaC in unit tests
        try {
            String sim = System.getProperty("newrelic.tempdir.cleanup.crac.present");
            if ("true".equalsIgnoreCase(sim)) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        final String CRAC_CLASS = "jdk.crac.Core";
        final String CRAC_RESOURCE = "jdk/crac/Core.class";

        ClassLoader[] loaders = new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                EmbeddedJarFilesImpl.class.getClassLoader(),
                ClassLoader.getSystemClassLoader(),
                null
        };

        for (ClassLoader cl : loaders) {
            try {
                Class.forName(CRAC_CLASS, false, cl);
                return true;
            } catch (ClassNotFoundException ex) {
                // try next
            } catch (LinkageError | SecurityException ex) {
                return true; // be conservative
            }
        }

        try {
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null && ctx.getResource(CRAC_RESOURCE) != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            ClassLoader cl = EmbeddedJarFilesImpl.class.getClassLoader();
            if (cl != null && cl.getResource(CRAC_RESOURCE) != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (ClassLoader.getSystemResource(CRAC_RESOURCE) != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

}
