/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.google.common.io.ByteStreams;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.JarCollectorConfig;
import com.newrelic.api.agent.Logger;
import com.newrelic.test.marker.Flaky;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentMatchers;

import javax.servlet.jsp.JspPage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JarCollectorServiceProcessorTest {

    static final String POM_PROPS_JAR_PATH = "com/newrelic/agent/service/module/pom-props-5.5.3.jar";

    /**
     * The path to the jar. The jar was created with "jar -cvfm jarTest.jar mainfestInfo.txt sample.txt". The
     * implementation-version is 2.0. The specification version is 3.0.
     */
    public static final String JAR_PATH = "com/newrelic/agent/service/module/jarTest.jar";
    /**
     * Path to a second jar. This jar has an implementation-version of 5.0 and a specification version of 6.0.
     */
    private static final String JAR_PATH_2 = "com/newrelic/agent/service/module/anotherJar.jar";

    private static final String THREADILIZER_PATH = "com/newrelic/agent/service/module/threadilizer.jar";

    /**
     * Text file which should not be picked up by the agent.
     */
    private static final String TXT_FILE = "com/newrelic/agent/service/module/manifestInfo.txt";

    private static final String EMBEDDED_JAR = "com/newrelic/agent/service/module/nr-cat-testnode-0.1.0.jar";

    /**
     * Gets the URL for the given resource.
     *
     * @param path Path starting at test.
     * @return The full path to the file.
     */
    static URL getURL(String path) {
        return ClassLoader.getSystemClassLoader().getResource(path);
    }

    static URL getJarURLInsideWar() throws IOException {
        File tmpFile = File.createTempFile("embedded_war", ".war");
        tmpFile.deleteOnExit();
        URL embeddedJarURL = JarCollectorServiceProcessorTest.getURL(EMBEDDED_JAR);
        try (FileOutputStream out = new FileOutputStream(tmpFile);
             InputStream in = embeddedJarURL.openStream()) {
            ByteStreams.copy(in, out);
        }

        return new URL(tmpFile.toURI().toURL().toExternalForm() + "!/lib/test-jar1-1.2.3.jar");
    }

    static URL getEmbeddedJarURL() throws MalformedURLException {
        return new URL(JarCollectorServiceProcessorTest.getURL(EMBEDDED_JAR).toExternalForm() + "!/lib/test-jar1-1.2.3.jar");
    }

    private AgentConfig getMockConfig() {
        AgentConfig agentConfig = mock(AgentConfig.class);
        JarCollectorConfig jarCollectorConfig = mock(JarCollectorConfig.class);
        when(agentConfig.getJarCollectorConfig()).thenReturn(jarCollectorConfig);
        return agentConfig;
    }

    @Test
    @Category( Flaky.class )
    // Flaky note: Somehow the elapsedMillis is sometimes < 4000, which probably shouldn't happen, but
    // I haven't investigated the warmup time of the SmoothBursty RateLimiter class
    //  Also, I put this in a loop and ran locally 1000 times and never saw a failure, so it
    // may be another strange timing issue in GH.
    public void applyWithRateLimit() throws URISyntaxException {
        AgentConfig config = getMockConfig();
        when(config.getJarCollectorConfig().getJarsPerSecond()).thenReturn(10);
        JarCollectorServiceProcessor target = spy(new JarCollectorServiceProcessor(mock(Logger.class), config));
        doReturn(mock(JarData.class)).when(target).tryProcessSingleURL(ArgumentMatchers.<URL>any());

        long startMillis = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            target.apply(getURL(TXT_FILE));
        }
        long elapsedMillis = System.currentTimeMillis() - startMillis;

        // 50 urls at 10 per second should take 5 second, but the rate limiter has a warm up period
        // preventing perfect accuracy so we give it some leniency for more reliable testing.
        assertTrue(elapsedMillis > 4000);
        verify(target, times(50)).apply(ArgumentMatchers.<URL>any());
    }

    @Test
    public void parseJarNameWithJarProtocol() throws MalformedURLException, URISyntaxException {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        URL url = new URL(
                "jar:file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar!/");
        assertEquals("xercesImpl-2.9.1-jbossas-1.jar", target.parseJarName(url));
    }

    @Test
    public void parseJarNameWithoutJarProtocolCurrentDir() throws MalformedURLException, URISyntaxException {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        URL url = new URL(
                "ftp:xercesImpl-2.9.1-jbossas-1.jar!/");
        assertEquals("xercesImpl-2.9.1-jbossas-1.jar", target.parseJarName(url));
    }

    @Test
    public void parseJarNameWithoutJarProtocolRootDir() throws MalformedURLException, URISyntaxException {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        URL url = new URL(
                "ftp:/xercesImpl-2.9.1-jbossas-1.jar!/");
        assertEquals("xercesImpl-2.9.1-jbossas-1.jar", target.parseJarName(url));
    }

    @Test
    public void testProcessJar1() {
        URL jarURL = ClassLoader.getSystemClassLoader().getResource(JAR_PATH);

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        JarData jarData = task.apply(jarURL);

        assertEquals("jarTest.jar", jarData.getName());
        assertEquals("2.0", jarData.getVersion());
    }

    @Test
    public void testProcessJar2() {
        URL jarURL = getURL(JAR_PATH_2);

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        JarData jarData = task.apply(jarURL);
        assertEquals("anotherJar.jar", jarData.getName());
        assertEquals("5.0", jarData.getVersion());
    }

    @Test
    public void isNotTemp() throws URISyntaxException {
        assertFalse(JarCollectorServiceProcessor.isTempFile(getURL(JAR_PATH)));
    }

    @Test
    public void isTemp() throws IOException {
        File temp = File.createTempFile("test", "dude");
        temp.deleteOnExit();
        assertTrue(JarCollectorServiceProcessor.isTempFile(temp));
    }

    @Test
    public void embeddedJar() throws IOException {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        JarInfo jarInfo = target.getJarInfoSafe(getEmbeddedJarURL());

        assertEquals("1.2.3", jarInfo.version);

        assertEquals("436bdbac7290779a1a89909827d8f24f632e3852", jarInfo.attributes.get("sha1Checksum"));
        assertEquals("5b1c62a33ea496f13e6b4ae77f9827e5ffc3b9121052e8946475ca02aa0aa65abef4c7d4a7fa1b792950caf11fbd0d8970cb6c4db3d9ca0da89a38a980a5b4ef",
                jarInfo.attributes.get("sha512Checksum"));
    }

    @Test
    public void embeddedWar() throws IOException {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        URL url = getJarURLInsideWar();
        assertTrue(url.toString().contains(".war!/"));
        JarInfo jarInfo = target.getJarInfoSafe(url);

        assertEquals("1.2.3", jarInfo.version);

        assertEquals("436bdbac7290779a1a89909827d8f24f632e3852", jarInfo.attributes.get("sha1Checksum"));
        assertEquals("5b1c62a33ea496f13e6b4ae77f9827e5ffc3b9121052e8946475ca02aa0aa65abef4c7d4a7fa1b792950caf11fbd0d8970cb6c4db3d9ca0da89a38a980a5b4ef",
                jarInfo.attributes.get("sha512Checksum"));
    }

    @Test
    public void getJarInfo_withoutPom() {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        JarInfo jarInfo = target.getJarInfoSafe(getURL(JAR_PATH));

        assertEquals("b82b735bc9ddee35c7fe6780d68f4a0256c4bd7a", jarInfo.attributes.get("sha1Checksum"));
        assertEquals("7101d4cdd4f68f81411f0150900f6b6cfc0c5547a8fb944daf7f5225033a0598ec93f16cb080e89ca0892362f71700e29a639eb2d750930a53168c43a735e050",
                jarInfo.attributes.get("sha512Checksum"));
    }

    @Test
    public void getJarInfo_noVersion() {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        JarInfo jarInfo = target.getJarInfoSafe(getURL(THREADILIZER_PATH));

        assertEquals("2cd63bbdc83562c6a26d7c96f13d11522541e352", jarInfo.attributes.get("sha1Checksum"));
        assertEquals(JarCollectorServiceProcessor.UNKNOWN_VERSION, jarInfo.version);
    }

    @Test
    public void getJarInfo_withPomProperties() {
        JarCollectorServiceProcessor target = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        JarInfo jarInfo = target.getJarInfoSafe(getURL(POM_PROPS_JAR_PATH));
        assertEquals("com.newrelic.pom.props", jarInfo.attributes.get("groupId"));
        assertEquals("pom-props", jarInfo.attributes.get("artifactId"));
        assertEquals("77cfa5e65ea08adcd7b502f1b85ae843172b5a9f", jarInfo.attributes.get("sha1Checksum"));
        assertEquals("5.5.3", jarInfo.attributes.get("version"));
    }

    @Test
    public void testProcessEmptyJar() throws Exception {
        File jar = File.createTempFile("test", "jar");
        jar.deleteOnExit();

        JarOutputStream out = new JarOutputStream(new FileOutputStream(jar));
        out.close();

        JarCollectorServiceProcessor processor = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        assertNotNull(processor.addJarAndVersion(jar.toURI().toURL(), null));

        // this hits a slightly different code path in addJarAndVersion
        AgentConfig agentConfig = getMockConfig();
        when(agentConfig.getIgnoreJars()).thenReturn(Collections.singletonList(jar.getName()));
        processor = new JarCollectorServiceProcessor(mock(Logger.class), agentConfig);
        assertNull(processor.addJarAndVersion(jar.toURI().toURL(), null));
    }

    @Test
    public void textFilesReturnNull() {
        URL txtURL = getURL(TXT_FILE);
        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor(mock(Logger.class), getMockConfig());
        assertNull(task.apply(txtURL));
    }

    @Test
    public void getVersion() throws IOException {
        // The jsp jar's version is not in the main attributes, it is tucked in the entries. Make sure we find it.
        String version = JarCollectorServiceProcessor.getVersion(
                EmbeddedJars.getJarInputStream(JspPage.class.getProtectionDomain().getCodeSource().getLocation()));
        assertNotNull(version);
    }

}
