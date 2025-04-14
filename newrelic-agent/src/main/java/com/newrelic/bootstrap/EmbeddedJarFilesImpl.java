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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;

public class EmbeddedJarFilesImpl implements EmbeddedJarFiles {

    private static final String[] INTERNAL_JAR_FILE_NAMES = new String[] { BootstrapLoader.AGENT_BRIDGE_JAR_NAME,
            BootstrapLoader.API_JAR_NAME, BootstrapLoader.WEAVER_API_JAR_NAME,  BootstrapLoader.NEWRELIC_SECURITY_AGENT,
            "opentelemetry-sdk-common-1.31.0", "opentelemetry-context-1.31.0", "opentelemetry-exporter-common-1.31.0", "opentelemetry-api-1.31.0",
            "opentelemetry-exporter-otlp-common-1.31.0", "opentelemetry-exporter-otlp-1.31.0", "opentelemetry-exporter-sender-okhttp-1.31.0",
            "opentelemetry-extension-incubator-1.31.0-alpha", "opentelemetry-sdk-extension-autoconfigure-spi-1.31.0", "opentelemetry-sdk-1.31.0",
            "opentelemetry-sdk-extension-autoconfigure-1.31.0", "opentelemetry-sdk-logs-1.31.0", "opentelemetry-sdk-metrics-1.31.0",
            "opentelemetry-sdk-trace-1.31.0", "micrometer-commons-1.12.1", "micrometer-observation-1.12.1", "okhttp-4.12.0"};

    public static final EmbeddedJarFiles INSTANCE = new EmbeddedJarFilesImpl();

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
        this.jarFileNames = jarFileNames;
    }

    @Override
    public File getJarFileInAgent(String jarNameWithoutExtension) throws IOException {
        return embeddedAgentJarFiles.get(jarNameWithoutExtension);
    }

    @Override
    public String[] getEmbeddedAgentJarFileNames() {
        return jarFileNames;
    }
}
