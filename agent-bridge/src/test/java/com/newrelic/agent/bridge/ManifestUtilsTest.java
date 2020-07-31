/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.JarTest1;
import com.newrelic.JarTest2;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ManifestUtilsTest {

    /**
     * The path to the jar. The jar was created with:
     *
     * "jar -cvfm jarTest.jar mainfestInfo.txt sample.txt JarTest1.class"
     *
     * The implementation-version is 5.0. The specification version is 6.0.
     */
    private static final String JAR_PATH_1 = "com/newrelic/agent/bridge/jarTest1.jar";

    /**
     * The path to the jar. The jar was created with:
     *
     * "jar -cvfm jarTest.jar mainfestInfo.txt sample.txt JarTest2.class"
     *
     * The Bundle-Version is 5.5.
     */
    private static final String JAR_PATH_2 = "com/newrelic/agent/bridge/jarTest2.jar";

    @Test
    public void testJar1NoFallback() throws Exception {
        String versionFromManifest = ManifestUtils.getVersionFromManifest(JarTest1.class, "this-wont-match", "default");

        assertNotNull(versionFromManifest);
        assertEquals("5.0", versionFromManifest);
    }

    @Test
    public void testJar1DoFallbackMatch() throws Exception {
        String versionFromManifest = ManifestUtils.getVersionFromManifest(JarTest1.class, "jarTest1", "default", true);

        assertNotNull(versionFromManifest);
        assertEquals("5.0", versionFromManifest);
    }

    @Test
    public void testJar1DoFallbackNoMatch() throws Exception {
        String versionFromManifest = ManifestUtils.getVersionFromManifest(JarTest1.class, "this-wont-match", "default",
                true);

        assertNotNull(versionFromManifest);
        assertEquals("default", versionFromManifest);
    }

    @Test
    public void testJar2NoFallback() throws Exception {
        String versionFromManifest = ManifestUtils.getVersionFromManifest(JarTest2.class, "this-wont-match", "default");

        assertNotNull(versionFromManifest);
        assertEquals("5.5", versionFromManifest);
    }

    @Test
    public void testJar2DoFallbackMatch() throws Exception {
        String versionFromManifest = ManifestUtils.getVersionFromManifest(JarTest1.class, "jarTest2", "default", true);

        assertNotNull(versionFromManifest);
        assertEquals("5.5", versionFromManifest);
    }

    @Test
    public void testJar2DoFallbackNoMatch() throws Exception {
        String versionFromManifest = ManifestUtils.getVersionFromManifest(JarTest1.class, "this-wont-match", "default",
                true);

        assertNotNull(versionFromManifest);
        assertEquals("default", versionFromManifest);
    }

}
