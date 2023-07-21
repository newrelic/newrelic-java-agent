package com.newrelic.weave.utils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JarUtilsTest {
    private static final byte [] FAKE_BYTES = "fake".getBytes();
    private static final Map<String, byte []> CLASS_BYTES_MAP = new HashMap<>();
    private static final Map<String, byte []> EXT_BYTES_MAP = new HashMap<>();

    static {
        CLASS_BYTES_MAP.put("class", FAKE_BYTES);
        EXT_BYTES_MAP.put("ext", FAKE_BYTES);
    }

    @Test
    public void createJarFile_withoutManifestOrExtensions_createsJarFile() throws IOException {
        File file = JarUtils.createJarFile("prefix", CLASS_BYTES_MAP);
        assertNotNull(file);
        assertTrue(file.exists());
    }

    @Test
    public void createJarFile_withoutExtensions_createsJarFile() throws IOException {
        File file = JarUtils.createJarFile("prefix", CLASS_BYTES_MAP, new Manifest());
        assertNotNull(file);
        assertTrue(file.exists());
    }

    @Test
    public void createJarFile_withAllParams_createsJarFile() throws IOException {
        File file = JarUtils.createJarFile("prefix", CLASS_BYTES_MAP, new Manifest(), EXT_BYTES_MAP);
        assertNotNull(file);
        assertTrue(file.exists());
    }
}
