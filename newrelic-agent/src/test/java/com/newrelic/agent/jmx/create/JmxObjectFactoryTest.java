/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.extension.YamlExtension;
import com.newrelic.agent.jmx.values.GlassfishJmxValues;
import com.newrelic.agent.jmx.values.JavaLangJmxMetrics;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.newrelic.agent.AgentHelper.getFile;

public class JmxObjectFactoryTest {

    private static final String SOLR_TEST_PATH = "com/newrelic/agent/jmx/create/solr_test.yml";
    private static final String NO_JMX_TEST_PATH = "com/newrelic/agent/jmx/create/no_jmx.yml";
    private static final String JMX_TEST_PATH = "com/newrelic/agent/jmx/create/test.yml";

    /**
     * A mock service manager.
     */
    private MockServiceManager serviceManager;

    @Before
    public void setUpAgent() {
        serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        AgentConfig config = AgentConfigImpl.createAgentConfig(Collections.EMPTY_MAP);

        ConfigService configService = ConfigServiceFactory.createConfigService(config, Collections.EMPTY_MAP);
        serviceManager.setConfigService(configService);
        serviceManager.setExtensionService(new ExtensionService(configService));
    }

    @Test
    public void jmxConfig6() throws MalformedObjectNameException {
        String versionSystemProp = System.getProperty("java.specification.version");
        double version = Double.parseDouble(versionSystemProp);
        if (version <= 1.5) {
            return;
        }

        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        List<JmxGet> objects = new ArrayList<>();
        List<JmxInvoke> invokes = new ArrayList<>();
        factory.getStartUpJmxObjects(objects, invokes);
        Assert.assertEquals(0, objects.size());
        JavaLangJmxMetrics metrics = new JavaLangJmxMetrics();
        factory.convertFramework(metrics, objects, invokes);
        Assert.assertEquals(metrics.getFrameworkMetrics().size(), objects.size());
        verify(objects, 4);

        objects.clear();
        factory.convertFramework(new GlassfishJmxValues(), objects, invokes);
        glassfishVerify(objects, invokes, 6);
    }

    private void glassfishVerify(List<JmxGet> gets, List<JmxInvoke> invokes, int minor) {
        Assert.assertEquals(3, gets.size());
        JmxGet one = gets.get(2);
        if (minor == 6) {
            Assert.assertEquals("amx:type=thread-pool-mon,pp=*,name=*", one.getObjectName().toString());
        } else {
            Assert.assertEquals("amx:type=thread-pool-mon,*", one.getObjectName().toString());
        }
        Assert.assertTrue(one.getAttributes().contains("currentthreadcount.count"));
        Assert.assertTrue(one.getAttributes().contains("currentthreadsbusy.count"));
        Assert.assertFalse(one.getAttributes().contains("NotAnAttribute"));

        Assert.assertEquals(1, invokes.size());
        JmxInvoke zero = invokes.get(0);
        Assert.assertEquals("amx-support:type=boot-amx", zero.getObjectName().toString());
        Assert.assertEquals("bootAMX", zero.getOperationName());
        Assert.assertEquals(0, zero.getParams().length);
        Assert.assertEquals(0, zero.getSignature().length);

    }

    private void verify(List<JmxGet> actual, int minor) {
        Assert.assertEquals(2, actual.size());
        JmxGet one = actual.get(1);
        Assert.assertEquals("java.lang:type=Threading", one.getObjectName().toString());
        Assert.assertTrue(one.getAttributes().contains("ThreadCount"));
        Assert.assertTrue(one.getAttributes().contains("TotalStartedThreadCount"));
        Assert.assertFalse(one.getAttributes().contains("NotAnAttribute"));

        JmxGet two = actual.get(0);
        Assert.assertEquals("java.lang:type=ClassLoading", two.getObjectName().toString());
        Assert.assertTrue(two.getAttributes().contains("LoadedClassCount"));
        Assert.assertTrue(two.getAttributes().contains("UnloadedClassCount"));
    }

    @Test
    public void testYml() throws Exception {
        List<JmxGet> actual = new ArrayList<>();
        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        Extension ext = createExtension(SOLR_TEST_PATH);
        factory.addExtension(ext, actual);
        verifyYml(actual);

    }

    private void verifyYml(List<JmxGet> actual) throws MalformedObjectNameException {
        StatsEngine stats = new StatsEngineImpl();
        Map<String, Float> values = new HashMap<>();
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        Assert.assertEquals(4, actual.size());
        JmxGet one = actual.get(3);
        Assert.assertEquals("solr*:type=queryResultCache,*", one.getObjectName().toString());
        Assert.assertTrue(one.getAttributes().contains("lookups"));

        // LOOKUPS
        values.put("lookups", 1f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/lookups").getTotal(), .001);

        // it should be monotonically increasing
        values.clear();
        values.put("lookups", 4f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(4f, stats.getStats("JMX/solr/test/lookups").getTotal(), .001);

        // HITS
        values.clear();
        values.put("hits", 1f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/hits").getTotal(), .001);

        // it should be monotonically increasing
        values.clear();
        values.put("hits", 4f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(4f, stats.getStats("JMX/solr/test/hits").getTotal(), .001);

        // hitratio
        values.clear();
        values.put("hitratio", 1f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/hitratio").getTotal(), .001);

        // it should be monotonically increasing
        values.clear();
        values.put("hitratio", 4f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(4f, stats.getStats("JMX/solr/test/hitratio").getTotal(), .001);

        // size
        values.clear();
        values.put("size", 1f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/size").getTotal(), .001);

        // it should be monotonically increasing
        values.clear();
        values.put("size", 4f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(4f, stats.getStats("JMX/solr/test/size").getTotal(), .001);

        // cumulative_hitratio
        values.clear();
        values.put("cumulative_hitratio", 1f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/cumulative_hitratio").getTotal(), .001);

        // it should be monotonically increasing
        values.clear();
        values.put("cumulative_hitratio", 4f);
        one.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(4f, stats.getStats("JMX/solr/test/cumulative_hitratio").getTotal(), .001);

        stats = new StatsEngineImpl();
        JmxGet two = actual.get(2);
        // lookups
        values.clear();
        values.put("lookups", 1f);
        two.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/lookups").getTotal(), .001);

        // it should be monotonically increasing
        values.clear();
        values.put("lookups", 4f);
        two.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(4f, stats.getStats("JMX/solr/test/lookups").getTotal(), .001);

        // size
        values.clear();
        values.put("size", 1f);
        two.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/size").getTotal(), .001);

        // it should be simple
        values.clear();
        values.put("size", 4f);
        two.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(5f, stats.getStats("JMX/solr/test/size").getTotal(), .001);

        // cumulative_hitratio
        values.clear();
        values.put("cumulative_hitratio", 1f);
        two.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(1f, stats.getStats("JMX/solr/test/cumulative_hitratio").getTotal(), .001);

        // it should be simple
        values.clear();
        values.put("cumulative_hitratio", 4f);
        two.recordStats(stats, createMap("solr:type=test", values), server);
        Assert.assertEquals(5f, stats.getStats("JMX/solr/test/cumulative_hitratio").getTotal(), .001);

    }

    @Test
    public void testYmlNoJMX() throws Exception {
        List<JmxGet> actual = new ArrayList<>();
        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        Extension ext = createExtension(NO_JMX_TEST_PATH);
        factory.addExtension(ext, actual);
        Assert.assertEquals(0, actual.size());

    }

    @Test
    public void testYmlJmx() throws Exception {
        List<JmxGet> actual = new ArrayList<>();
        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        Extension ext = createExtension(JMX_TEST_PATH);
        factory.addExtension(ext, actual);
        Assert.assertEquals(1, actual.size());

    }

    private Map<ObjectName, Map<String, Float>> createMap(String name, Map<String, Float> values)
            throws MalformedObjectNameException {
        Map<ObjectName, Map<String, Float>> output = new HashMap<>();
        output.put(new ObjectName(name), values);
        return output;
    }

    private Extension createExtension(String path) throws Exception {
        File file = getFile(path);
        BaseConfig config = JmxYmlParserTest.readYml(file);
        return new YamlExtension(getClass().getClassLoader(), "Test", config.getProperties(), true);

    }

}
