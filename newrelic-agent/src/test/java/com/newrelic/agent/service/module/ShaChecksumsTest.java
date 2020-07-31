/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.Test;

public class ShaChecksumsTest {

    @Test
    public void test() throws NoSuchAlgorithmException, IOException {
        URL url = JarCollectorServiceProcessorTest.getURL(JarCollectorServiceProcessorTest.JAR_PATH);
        Assert.assertEquals("b82b735bc9ddee35c7fe6780d68f4a0256c4bd7a", ShaChecksums.computeSha(url));
    }
    
    @Test
    public void testJarWithinJar() throws NoSuchAlgorithmException, IOException {
        Assert.assertEquals("436bdbac7290779a1a89909827d8f24f632e3852", ShaChecksums.computeSha(JarCollectorServiceProcessorTest.getEmbeddedJarURL()));
    }
}
