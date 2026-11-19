/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class JarUtil {
    /**
     * The fixed entry time Gradle and Shadow use for archives with preserveFileTimestamps = false (1980-02-01 00:00).
     * It is taken from Shadow so there is one definition of it. Used where the build writes zip entries itself
     * ({@link #setConstantEntryTimes}); the Shadow transformers call
     * {@link TransformerContext#getEntryTimestamp} directly.
     */
    public static final long CONSTANT_ENTRY_TIME = TransformerContext.getEntryTimestamp(false, 0L);

    /**
     * Rewrites the zip in place with the same entries, in the same order and with the same contents, but with every
     * entry time set to {@link #CONSTANT_ENTRY_TIME}. Used for zips written by tools that stamp the time of the run
     * (the JDK 11 javadoc tool's *-search-index.zip files).
     */
    public static void setConstantEntryTimes(File zipFile) throws IOException {
        List<String> names = new ArrayList<>();
        List<byte[]> contents = new ArrayList<>();
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                names.add(entry.getName());
                try (InputStream in = zip.getInputStream(entry)) {
                    contents.add(Streams.readAll(in));
                }
            }
        }

        try (OutputStream out = Files.newOutputStream(zipFile.toPath()); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < names.size(); i++) {
                ZipEntry entry = new ZipEntry(names.get(i));
                entry.setTime(CONSTANT_ENTRY_TIME);
                zip.putNextEntry(entry);
                zip.write(contents.get(i));
                zip.closeEntry();
            }
        }
    }

    public static File getNewRelicJar(Project newrelicAgentProject) {
        Optional<Task> jarTaskSet = newrelicAgentProject.getTasksByName("newrelicVersionedAgentJar", false).stream().findAny();
        if (!jarTaskSet.isPresent()) {
            throw new GradleException("Could not find task: newrelicVersionedAgentJar in project: " + newrelicAgentProject.getName());
        }

        Task jarTask = jarTaskSet.get();
        if (!(jarTask instanceof Jar)) {
            throw new GradleException("task newrelicVersionedAgentJar is not a Jar task.");
        }

        return ((Jar)jarTask).getArchiveFile().get().getAsFile();
    }
}
