/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.logging.Logging;
import org.slf4j.Logger;
import shadow.org.apache.tools.zip.ZipEntry;
import shadow.org.apache.tools.zip.ZipOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * In order to shadow log4j2 defensively, we move Log4j2Plugins.dat to another location.
 * This transformer is used to change the path of the file in the jar.
 */
public class Log4j2PluginFileMover implements Transformer {
    public static final String LOG4J2PLUGINS_ORIGINAL_LOCATION = "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";
    public static final String LOG4J2PLUGINS_NEW_LOCATION = "META-INF/com/newrelic/agent/deps/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat";

    private File tempContents;
    private long size;
    private long lastModified;
    private static final Logger logger = Logging.getLogger(Log4j2PluginFileMover.class);

    @Override
    public boolean canTransformResource(FileTreeElement element) {
        if (element.getPath().equals(LOG4J2PLUGINS_ORIGINAL_LOCATION)) {
            logger.info("Will relocate Log4j2Plugins.dat");
            lastModified = element.getLastModified();
            return true;
        }

        return false;
    }

    @Override
    public void transform(TransformerContext context) {
        try {
            tempContents = File.createTempFile("Log4jPlugins", "temp");
        } catch (IOException e) {
            tempContents = null;
            throw new RuntimeException("Unable to create temp file", e);
        }

        try (OutputStream tempStream = new FileOutputStream(tempContents)) {
            size = Streams.copy(context.getIs(), tempStream);
        } catch (IOException e) {
            tempContents = null;
            throw new RuntimeException("Unable to write to temp file", e);
        }
    }

    @Override
    public boolean hasTransformedResource() {
        return tempContents != null;
    }

    @Override
    public void modifyOutputStream(ZipOutputStream jos, boolean preserveFileTimestamps) {
        ZipEntry entry = new ZipEntry(LOG4J2PLUGINS_NEW_LOCATION);
        entry.setSize(size);
        if (preserveFileTimestamps) {
            entry.setTime(lastModified);
        }
        try {
            jos.putNextEntry(entry);
        } catch (IOException e) {
            throw new RuntimeException("Unable to add entry to the jar", e);
        }

        try (InputStream fromFile = new FileInputStream(tempContents)) {
            Streams.copy(fromFile, jos);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write Log4j2Plugins.dat to the jar", e);
        }

        logger.info("Successfully wrote Log4j2Plugins.dat to META-INF/com/newrelic/");
    }
}
