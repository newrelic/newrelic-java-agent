/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClassResourceTests {

    @Test
    public void testJar() throws IOException {
        URL jarURL = Thread.currentThread().getContextClassLoader().getResource("testjar.jar");
        assertClasses(jarURL);
    }

    @Test
    public void testDirectory() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File classpathDir = new File(tempDir, "classp");
        try {
            File comDir = new File(classpathDir, "com");
            File exampleDir = new File(comDir, "example");
            assertTrue(exampleDir.mkdirs());

            File one = new File(exampleDir, "One.class");
            assertTrue(one.createNewFile());

            File two = new File(exampleDir, "Two.class");
            assertTrue(two.createNewFile());

            URL dirURL = classpathDir.toURI().toURL();
            assertClasses(dirURL);
        } finally {
            deleteDir(classpathDir);
        }
    }

    private static void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    private void assertClasses(URL url) throws IOException {
        List<ClassResource> classResources = new ArrayList<>();
        ClassResource.fromURL(url, classResources);
        assertEquals(2, classResources.size());

        Set<String> resourceNames = new HashSet<>();
        for(ClassResource classResource : classResources) {
            resourceNames.add(classResource.resourceName);
        }
        assertTrue(resourceNames.contains("com/example/One.class"));
        assertTrue(resourceNames.contains("com/example/Two.class"));
    }
}
