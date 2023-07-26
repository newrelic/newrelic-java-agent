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
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.security.NewRelicSecurity;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.config.JarResource;

import static com.newrelic.bootstrap.BootstrapLoader.AGENT_BRIDGE_DATASTORE_JAR_NAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

        verify(inst, Mockito.atLeastOnce()).appendToBootstrapClassLoaderSearch(Mockito.any(JarFile.class));
    }

    @Test
    public void apiClassTransformerInnerClass_constructor_andTransform() throws ClassNotFoundException, IllegalClassFormatException {
        byte [] bytes = "test".getBytes();
        ClassLoader mockClassloader = Mockito.mock(ClassLoader.class);
        ProtectionDomain mockProtectionDomain = Mockito.mock(ProtectionDomain.class);
        BootstrapLoader.ApiClassTransformer apiClassTransformer = new BootstrapLoader.ApiClassTransformer("appname", bytes);

        assertEquals(apiClassTransformer.transform(mockClassloader, "appname", Class.forName("java.lang.String"), mockProtectionDomain, bytes), bytes);
        assertNull(apiClassTransformer.transform(mockClassloader, null, Class.forName("java.lang.String"), mockProtectionDomain, bytes));
    }

    @Test
    @Ignore
    public void forceCorrectNewRelicApi_addsTransformerToInstrumentation() throws IOException {
        Instrumentation mockInstr = Mockito.mock(Instrumentation.class);
        BootstrapLoader.forceCorrectNewRelicApi(mockInstr);
        verify(mockInstr, times(1)).addTransformer(Mockito.any(BootstrapLoader.ApiClassTransformer.class), eq(true));
    }

    @Test
    @Ignore
    public void forceCorrectNewRelicSecurityApi_addsTransformerToInstrumentation() throws IOException, UnmodifiableClassException {
        Instrumentation mockInstr = Mockito.mock(Instrumentation.class);
        BootstrapLoader.forceCorrectNewRelicSecurityApi(mockInstr);
        verify(mockInstr, times(1)).addTransformer(Mockito.any(BootstrapLoader.ApiClassTransformer.class), eq(true));
        verify(mockInstr, times(1)).retransformClasses(NewRelicSecurity.class);
    }

    @Test
    @Ignore
    public void getDatastoreJarURL_returnsCorrectURL() throws IOException, ClassNotFoundException {
        assertNotNull(BootstrapLoader.getDatastoreJarURL());
        assertTrue(BootstrapLoader.getDatastoreJarURL().toString().contains(AGENT_BRIDGE_DATASTORE_JAR_NAME));
    }

    @Test
    @Ignore
    public void getJarURLs_returnsCorrectCollectionOfUrls() throws IOException, ClassNotFoundException {
        Collection<URL> urlList = BootstrapLoader.getJarURLs();
        assertEquals(6, urlList.size());
    }

    @Test
    @Ignore
    public void load_isJavaSqlLoadedIsFalse_addsClassesToClassLoader() throws IOException, UnmodifiableClassException {
        Instrumentation mockInstr = Mockito.mock(Instrumentation.class);
        BootstrapLoader.load(mockInstr, false);
        verify(mockInstr, times(6)).appendToBootstrapClassLoaderSearch(any());
    }

    @Test
    @Ignore
    public void load_isJavaSqlLoadedIsTrue_addsClassesToClassLoader() throws IOException, UnmodifiableClassException {
        Instrumentation mockInstr = Mockito.mock(Instrumentation.class);
        BootstrapLoader.load(mockInstr, true);
        verify(mockInstr, times(5)).appendToBootstrapClassLoaderSearch(any());
    }

    @Test
    public void getTempDir_withSysPropSet_returnsNull() throws IOException, UnmodifiableClassException {
        System.setProperty("newrelic.tempdir", "./");
        assertNotNull(BootstrapLoader.getTempDir());
        System.clearProperty("newrelic.tempdir");

    }

    @Test
    public void getTempDir_withoutSysPropSet_returnsNull() throws IOException, UnmodifiableClassException {
        assertNull(BootstrapLoader.getTempDir());
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
