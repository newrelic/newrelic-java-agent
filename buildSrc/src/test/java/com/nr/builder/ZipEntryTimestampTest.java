/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.file.FileTreeElement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The build must not put the time it ran into any jar entry. These cover the places the build writes entry times itself.
 */
public class ZipEntryTimestampTest {
    private static final long ONE_YEAR_AGO = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void constantEntryTime_isTheFixedTimeShadowAndGradleUse() {
        assertEquals(TransformerContext.getEntryTimestamp(false, 12345L), JarUtil.CONSTANT_ENTRY_TIME);
        // 1980-02-01 is after the start of the zip time range, so the time is representable.
        assertTrue(JarUtil.CONSTANT_ENTRY_TIME < ONE_YEAR_AGO);
    }

    @Test
    public void setConstantEntryTimes_keepsNamesOrderAndContentsAndFixesTheTimes() throws Exception {
        File zip = temporaryFolder.newFile("search-index.zip");
        try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            for (String name : new String[] { "b.json", "a.json", "dir/c.json" }) {
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(System.currentTimeMillis());
                out.putNextEntry(entry);
                out.write(("contents of " + name).getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }

        JarUtil.setConstantEntryTimes(zip);

        List<String> names = new ArrayList<>();
        try (ZipFile result = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> entries = result.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                names.add(entry.getName());
                assertEquals(entry.getName(), JarUtil.CONSTANT_ENTRY_TIME, entry.getTime());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream in = result.getInputStream(entry)) {
                    Streams.copy(in, bytes);
                }
                assertEquals("contents of " + entry.getName(), new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            }
        }
        assertEquals(java.util.Arrays.asList("b.json", "a.json", "dir/c.json"), names);
    }

    @Test
    public void setConstantEntryTimes_givesIdenticalBytesWhateverTheOriginalTimes() throws Exception {
        File first = zipWithEntryTime("first.zip", ONE_YEAR_AGO);
        File second = zipWithEntryTime("second.zip", System.currentTimeMillis());

        JarUtil.setConstantEntryTimes(first);
        JarUtil.setConstantEntryTimes(second);

        assertArrayEquals(Files.readAllBytes(first.toPath()), Files.readAllBytes(second.toPath()));
    }

    @Test
    public void dependencyPatcher_writesEntriesWithTheFixedTimeUnlessTimestampsArePreserved() throws Exception {
        byte[] classBytes;
        try (InputStream in = getClass().getResourceAsStream("ZipEntryTimestampTest.class")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Streams.copy(in, bytes);
            classBytes = bytes.toByteArray();
        }
        DependencyPatcher patcher = new DependencyPatcher();
        patcher.transform(new TransformerContext("com/newrelic/agent/deps/Example.class", new ByteArrayInputStream(classBytes)));

        assertEquals(JarUtil.CONSTANT_ENTRY_TIME, entryTime(patcher::modifyOutputStream, "com/newrelic/agent/deps/Example.class", false));

        long before = System.currentTimeMillis();
        long preserved = entryTime(patcher::modifyOutputStream, "com/newrelic/agent/deps/Example.class", true);
        assertTrue("with preserveFileTimestamps the time of the build is used", preserved >= before - 4000);
    }

    @Test
    public void log4j2PluginFileMover_writesTheEntryWithTheFixedTimeUnlessTimestampsArePreserved() throws Exception {
        Log4j2PluginFileMover mover = new Log4j2PluginFileMover();
        FileTreeElement element = (FileTreeElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { FileTreeElement.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getPath")) {
                        return Log4j2PluginFileMover.LOG4J2PLUGINS_ORIGINAL_LOCATION;
                    }
                    if (method.getName().equals("getLastModified")) {
                        return ONE_YEAR_AGO;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        assertTrue(mover.canTransformResource(element));
        mover.transform(new TransformerContext(Log4j2PluginFileMover.LOG4J2PLUGINS_ORIGINAL_LOCATION,
                new ByteArrayInputStream("plugins".getBytes(StandardCharsets.UTF_8))));

        assertEquals(JarUtil.CONSTANT_ENTRY_TIME, entryTime(mover::modifyOutputStream, Log4j2PluginFileMover.LOG4J2PLUGINS_NEW_LOCATION, false));
        // Within the zip's two second resolution (and the local time zone conversion), the original modification time is kept.
        assertTrue(Math.abs(ONE_YEAR_AGO - entryTime(mover::modifyOutputStream, Log4j2PluginFileMover.LOG4J2PLUGINS_NEW_LOCATION, true)) <= 4000);
    }

    private interface OutputStreamModifier {
        void modify(ZipOutputStream jos, boolean preserveFileTimestamps);
    }

    private long entryTime(OutputStreamModifier modifier, String entryName, boolean preserveFileTimestamps) throws Exception {
        File zip = temporaryFolder.newFile();
        try (ZipOutputStream out = new ZipOutputStream(zip)) {
            modifier.modify(out, preserveFileTimestamps);
        }
        try (ZipFile result = new ZipFile(zip)) {
            return result.getEntry(entryName).getTime();
        }
    }

    private File zipWithEntryTime(String name, long time) throws Exception {
        File zip = temporaryFolder.newFile(name);
        try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            ZipEntry entry = new ZipEntry("index.json");
            entry.setTime(time);
            out.putNextEntry(entry);
            out.write("same".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return zip;
    }
}
