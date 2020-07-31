/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.newrelic.agent.Agent;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.config.JarResource;

public class BootstrapLoaderTest {

    @Test
    public void testCreateMixinJar() throws IOException {
        final String agentDirName = "newrelic-agent/";
        final String nrJarSubPath = "build/newrelicJar/newrelic.jar";
        final int agentDirNameLen = agentDirName.length();
        String agentDirPath = null;

        // See if we can find a newrelic.jar to use by discovering the fully rooted
        // path of a java agent build directory by sniffing the class path.
        String[] path = System.getProperty("java.class.path").split(File.pathSeparator);
        for (String s : path) {
            int n = s.indexOf(agentDirName);
            if (n > 0) {
                n += agentDirNameLen;
                agentDirPath = s.substring(0, n);
                break;
            }
        }

        if (agentDirPath == null) {
            // No newrelic.jar can be found. Test passes, vacuous case
            return;
        }

        String nrJarPath = agentDirPath + "/" + nrJarSubPath;
        File nrJarFile = new File(nrJarPath);
        if (nrJarFile.canRead()) {
            doTestCreateMixinJar(nrJarFile);
        }
    }

    private void doTestCreateMixinJar(File nrJarFile) throws IOException {

        Instrumentation inst = Mockito.mock(Instrumentation.class);
        JarFile agentJarFile = new JarFile(nrJarFile);
        JarResource agentJarResource = getJarResource(agentJarFile);
        URL agentJarUrl = new URL("file://" + nrJarFile.getPath());

        // Finally, call the code we're trying to test.
        Agent.addMixinInterfacesToBootstrap(agentJarResource, agentJarUrl, inst);

        Mockito.verify(inst, Mockito.atLeastOnce()).appendToBootstrapClassLoaderSearch(Mockito.any(JarFile.class));
    }

    private JarResource getJarResource(final JarFile agentJarFile) {
        return new JarResource() {

            @Override
            public void close() throws IOException {
                agentJarFile.close();
            }

            @Override
            public InputStream getInputStream(String name) throws IOException {
                ZipEntry entry = agentJarFile.getJarEntry(name);
                return agentJarFile.getInputStream(entry);
            }

            @Override
            public long getSize(String name) {
                ZipEntry entry = agentJarFile.getJarEntry(name);
                return entry.getSize();
            }
        };
    }
}
