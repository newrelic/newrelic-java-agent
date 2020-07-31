/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.extension.ExtensionServiceTest;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.jsp.JspPage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;

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

    static URL getEmbeddedJarURL(String extension) throws IOException {
        File tmpFile = File.createTempFile("embedded_" + extension, '.' + extension);
        tmpFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(tmpFile);
        URL embeddedJarURL = JarCollectorServiceProcessorTest.getURL(EMBEDDED_JAR);
        InputStream in = embeddedJarURL.openStream();
        ByteStreams.copy(in, out);
        out.close();
        return new URL(tmpFile.toURI().toURL().toExternalForm() + "!/lib/test-jar1-1.2.3.jar");
    }

    static URL getEmbeddedJarURL() throws MalformedURLException {
        return new URL(JarCollectorServiceProcessorTest.getURL(EMBEDDED_JAR).toExternalForm() + "!/lib/test-jar1-1.2.3.jar");
    }

    @Before
    public void setup() {
        Map<String, Object> confProps = new HashMap<>();
        confProps.put(AgentConfigImpl.APP_NAME, "Hello");
        AgentConfig config = AgentConfigImpl.createAgentConfig(confProps);
        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        MockConfigService service = new MockConfigService(AgentConfigImpl.createAgentConfig(confProps));
        service.setAgentConfig(config);
        manager.setConfigService(service);
        EnvironmentService envservice = new EnvironmentServiceImpl();
        manager.setEnvironmentService(envservice);
        RPMServiceManager rpmService = new MockRPMServiceManager();
        manager.setRPMServiceManager(rpmService);
        HarvestService harvest = new HarvestServiceImpl();
        manager.setHarvestService(harvest);
    }

    @Test
    public void parseJarNameWithJarProtocol() throws MalformedURLException, URISyntaxException {
        URL url = new URL(
                "jar:file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar!/");
        Assert.assertEquals("xercesImpl-2.9.1-jbossas-1.jar", JarCollectorServiceProcessor.parseJarName(url));
    }

    @Test
    public void parseJarNameWithoutJarProtocolCurrentDir() throws MalformedURLException, URISyntaxException {
        URL url = new URL(
                "ftp:xercesImpl-2.9.1-jbossas-1.jar!/");
        Assert.assertEquals("xercesImpl-2.9.1-jbossas-1.jar", JarCollectorServiceProcessor.parseJarName(url));
    }

    @Test
    public void parseJarNameWithoutJarProtocolRootDir() throws MalformedURLException, URISyntaxException {
        URL url = new URL(
                "ftp:/xercesImpl-2.9.1-jbossas-1.jar!/");
        Assert.assertEquals("xercesImpl-2.9.1-jbossas-1.jar", JarCollectorServiceProcessor.parseJarName(url));
    }

    @Test
    public void testProcessJar1() throws Exception {
        URL jarURL = ClassLoader.getSystemClassLoader().getResource(JAR_PATH);

        List<URL> urls = Collections.singletonList(jarURL);
        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor();
        List<Jar> jars = task.processModuleData(urls, false);

        Assert.assertEquals(1, jars.size());
        Jar jar = jars.get(0);
        Assert.assertEquals("jarTest.jar", jar.getName());
        Assert.assertEquals("2.0", jar.getVersion());
    }

    @Test
    public void testProcessJar2TwoArrays() throws Exception {
        URL jarURL = getURL(JAR_PATH_2);

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor();
        List<URL> urls = new ArrayList<>();
        urls.add(jarURL);
        List<Jar> jars = task.processModuleData(urls, false);

        Assert.assertEquals(1, jars.size());
        Jar jar = jars.get(0);
        Assert.assertEquals("anotherJar.jar", jar.getName());
        Assert.assertEquals("5.0", jar.getVersion());

        URL nextJarURL = getURL(JAR_PATH);

        urls.add(nextJarURL);
        jars = task.processModuleData(urls, false);

        // should only be one since the other returned last time
        Assert.assertEquals(1, jars.size());

        jar = jars.get(0);
        Assert.assertEquals("jarTest.jar", jar.getName());
        Assert.assertEquals("2.0", jar.getVersion());
    }

    @Test
    public void testProcessSameJarTwice() throws Exception {
        URL jarURL = getURL(JAR_PATH_2);
        URL firstJarURL = new URL(jarURL.getProtocol(), jarURL.getHost(), jarURL.getFile());

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor();
        List<URL> urls = new ArrayList<>();
        urls.add(firstJarURL);
        List<Jar> jars = task.processModuleData(urls, false);

        Assert.assertEquals(1, jars.size());
        Jar jar = jars.get(0);
        Assert.assertEquals("anotherJar.jar", jar.getName());
        Assert.assertEquals("5.0", jar.getVersion());

        URL secondJarURL = new URL(jarURL.getProtocol(), jarURL.getHost(), jarURL.getFile());
        urls.clear();
        urls.addAll(Arrays.asList(secondJarURL, firstJarURL));
        jars = task.processModuleData(urls, false);

        // should be 0 because we've already processed this jar
        Assert.assertEquals(0, jars.size());
    }

    @Test
    public void isNotTemp() throws URISyntaxException {
        Assert.assertFalse(JarCollectorServiceProcessor.isTempFile(getURL(JAR_PATH)));
    }

    @Test
    public void isTemp() throws URISyntaxException, IOException {
        File temp = File.createTempFile("test", "dude");
        try {
            Assert.assertTrue(JarCollectorServiceProcessor.isTempFile(temp));
        } finally {
            temp.delete();
        }
    }

    @Test
    public void embeddedJar() throws IOException {
        JarInfo jarInfo = JarCollectorServiceProcessor.getJarInfoSafe(getEmbeddedJarURL());

        Assert.assertEquals("1.2.3", jarInfo.version);

        Assert.assertEquals("436bdbac7290779a1a89909827d8f24f632e3852", jarInfo.attributes.get("sha1Checksum"));
        Assert.assertEquals("5b1c62a33ea496f13e6b4ae77f9827e5ffc3b9121052e8946475ca02aa0aa65abef4c7d4a7fa1b792950caf11fbd0d8970cb6c4db3d9ca0da89a38a980a5b4ef",
                jarInfo.attributes.get("sha512Checksum"));
    }

    @Test
    public void embeddedWar() throws IOException {
        URL url = getEmbeddedJarURL("war");
        Assert.assertTrue(url.toString().contains(".war!/"));
        JarInfo jarInfo = JarCollectorServiceProcessor.getJarInfoSafe(url);

        Assert.assertEquals("1.2.3", jarInfo.version);

        Assert.assertEquals("436bdbac7290779a1a89909827d8f24f632e3852", jarInfo.attributes.get("sha1Checksum"));
        Assert.assertEquals("5b1c62a33ea496f13e6b4ae77f9827e5ffc3b9121052e8946475ca02aa0aa65abef4c7d4a7fa1b792950caf11fbd0d8970cb6c4db3d9ca0da89a38a980a5b4ef",
                jarInfo.attributes.get("sha512Checksum"));
    }

    @Test
    public void getJarInfo_withoutPom() throws IOException {
        JarInfo jarInfo = JarCollectorServiceProcessor.getJarInfoSafe(getURL(JAR_PATH));

        Assert.assertEquals("b82b735bc9ddee35c7fe6780d68f4a0256c4bd7a", jarInfo.attributes.get("sha1Checksum"));
        Assert.assertEquals("7101d4cdd4f68f81411f0150900f6b6cfc0c5547a8fb944daf7f5225033a0598ec93f16cb080e89ca0892362f71700e29a639eb2d750930a53168c43a735e050",
                jarInfo.attributes.get("sha512Checksum"));
    }

    @Test
    public void getJarInfo_noVersion() throws IOException {
        JarInfo jarInfo = JarCollectorServiceProcessor.getJarInfoSafe(getURL(THREADILIZER_PATH));

        Assert.assertEquals("2cd63bbdc83562c6a26d7c96f13d11522541e352", jarInfo.attributes.get("sha1Checksum"));
        Assert.assertEquals(JarCollectorServiceProcessor.UNKNOWN_VERSION, jarInfo.version);
    }

    @Test
    public void getJarInfo_withPomProperties() throws IOException {
        JarInfo jarInfo = JarCollectorServiceProcessor.getJarInfoSafe(getURL(POM_PROPS_JAR_PATH));

        Assert.assertEquals("com.newrelic.pom.props", jarInfo.attributes.get("groupId"));
        Assert.assertEquals("pom-props", jarInfo.attributes.get("artifactId"));
        Assert.assertEquals("77cfa5e65ea08adcd7b502f1b85ae843172b5a9f", jarInfo.attributes.get("sha1Checksum"));
        Assert.assertEquals("5.5.3", jarInfo.attributes.get("version"));
    }

    @Test
    public void testProcessJar2OneArray() throws Exception {
        URL jarURL = getURL(JAR_PATH_2);

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor();
        List<URL> urls = new ArrayList<>();
        urls.add(jarURL);
        List<Jar> jars = task.processModuleData(urls, false);

        Assert.assertEquals(1, jars.size());
        Jar jar = jars.get(0);
        Assert.assertEquals("anotherJar.jar", jar.getName());
        Assert.assertEquals("5.0", jar.getVersion());

        URL nextJarURL = getURL(JAR_PATH);

        urls.clear();
        urls.addAll(Arrays.asList(jarURL, nextJarURL));
        jars = task.processModuleData(urls, false);

        // should only be one since the other returned last time
        Assert.assertEquals(1, jars.size());

        jar = jars.get(0);
        Assert.assertEquals("jarTest.jar", jar.getName());
        Assert.assertEquals("2.0", jar.getVersion());
    }

    @Test
    public void testProcessEmptyJar() throws Exception {
        File jar = File.createTempFile("test", "jar");
        jar.deleteOnExit();

        JarOutputStream out = new JarOutputStream(new FileOutputStream(jar));
        out.close();

        JarCollectorServiceProcessor processor = new JarCollectorServiceProcessor();

        List<Jar> jars = new ArrayList<>();
        Assert.assertTrue(processor.addJarAndVersion(jar.toURI().toURL(), null, jars));

        // this hits a slightly different code path in addJarAndVersion
        processor = new JarCollectorServiceProcessor(Collections.singletonList(jar.getName()));
        Assert.assertFalse(processor.addJarAndVersion(jar.toURI().toURL(), null, jars));
    }

    @Test
    public void testProcessJarReturnAll() throws Exception {
        URL jarURL = getURL(JAR_PATH_2);

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor();
        List<URL> urls = new ArrayList<>();
        urls.add(jarURL);
        List<Jar> jars = task.processModuleData(urls, false);

        Assert.assertEquals(1, jars.size());
        Jar jar = jars.get(0);
        Assert.assertEquals("anotherJar.jar", jar.getName());
        Assert.assertEquals("5.0", jar.getVersion());

        URL nextJarURL = getURL(JAR_PATH);

        urls.clear();
        urls.addAll(Arrays.asList(jarURL, nextJarURL));
        jars = task.processModuleData(urls, true);

        Assert.assertEquals(2, jars.size());

        Assert.assertTrue(jars.contains(newJar("anotherJar.jar", "5.0")));
        Assert.assertTrue(jars.contains(newJar("jarTest.jar", "2.0")));
    }

    private Jar newJar(String name, String version) {
        return new Jar(name, new JarInfo(version, ImmutableMap.<String, String>of()));
    }

    @Test
    public void testProcessTwoJars() throws Exception {
        URL jarURL1 = getURL(JAR_PATH);
        URL jarURL2 = getURL(JAR_PATH_2);

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor();

        List<URL> urls = new ArrayList<>(Arrays.asList(jarURL1, jarURL2));
        List<Jar> jars = task.processModuleData(urls, false);

        Assert.assertEquals(2, jars.size());
        Assert.assertTrue(jars.contains(newJar("anotherJar.jar", "5.0")));
        Assert.assertTrue(jars.contains(newJar("jarTest.jar", "2.0")));
    }

    @Test
    public void testTextFile() throws MalformedURLException {
        URL txtURL = getURL(TXT_FILE);

        JarCollectorServiceProcessor task = new JarCollectorServiceProcessor();
        List<URL> urls = Collections.singletonList(txtURL);
        List<Jar> jars = task.processModuleData(urls, false);

        Assert.assertEquals(0, jars.size());
    }

    @Test
    public void weaveModules() {
        URL url = getClass().getResource('/' + ExtensionServiceTest.class.getPackage().getName().replace('.', '/') + '/'
                + ExtensionServiceTest.WEAVE_INSTRUMENTATION);

        Map<File, WeavePackageConfig> weaveConfigurations =
                ImmutableMap.of(new File(url.getFile()), WeavePackageConfig.builder().name("jms").version(6.66f).source(
                        url.getPath()).build());
        Collection<Jar> jars = JarCollectorServiceProcessor.getWeaveJars(weaveConfigurations);

        Jar jar = jars.iterator().next();
        Assert.assertEquals("jms", jar.getName());
        Assert.assertEquals("6.66", jar.getVersion());

        Assert.assertEquals(url.getFile(), jar.getJarInfo().attributes.get("weaveFile"));
        Assert.assertEquals("10ce178a632add8d5a98442a9cf1220f34c95874", jar.getJarInfo().attributes.get("sha1Checksum"));
    }

    @Test
    public void getVersion() throws IOException {
        // The jsp jar's version is not in the main attributes, it is tucked in the entries. Make sure we find it.
        String version = JarCollectorServiceProcessor.getVersion(
                EmbeddedJars.getJarInputStream(JspPage.class.getProtectionDomain().getCodeSource().getLocation()));
        Assert.assertNotNull(version);
    }

}
