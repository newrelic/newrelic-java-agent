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
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.MalformedObjectNameException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmxObjectJava6UpTest {

    /** A mock service manager. */
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
    public void testJmxGetJava6UpBasic() throws MalformedObjectNameException {
        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        Map<JmxType, List<String>> mapping = new HashMap<>();
        List<String> atts = new ArrayList<>();
        atts.add("Name");
        atts.add("Date");
        atts.add("Time");
        mapping.put(JmxType.SIMPLE, atts);
        String name = "solr*:type=queryResultCache,*";
        JmxGet object = new JmxSingleMBeanGet(name, null, factory.getSafeObjectName(name), mapping, null);
        Assert.assertEquals("solr*:type=queryResultCache,*", object.getObjectNameString());
        Assert.assertEquals("solr*:type=queryResultCache,*", factory.getSafeObjectName("solr*:type=queryResultCache,*"));
    }

    @Test
    public void testJmxGetJava6UpRegex() throws MalformedObjectNameException {
        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        Map<JmxType, List<String>> mapping = new HashMap<>();
        List<String> atts = new ArrayList<>();
        atts.add("Name");
        atts.add("Date");
        atts.add("Time");
        mapping.put(JmxType.SIMPLE, atts);
        String name = "solr*:type=queryResultCache, property=hello";
        JmxGet object = new JmxSingleMBeanGet(name, null, factory.getSafeObjectName(name), mapping, null);
        Assert.assertEquals("solr*:type=queryResultCache, property=hello", object.getObjectNameString());
        Assert.assertEquals("solr*:type=queryResultCache, property=hello",
                factory.getSafeObjectName("solr*:type=queryResultCache, property=hello"));
        Assert.assertEquals("solr*: property=hello,type=queryResultCache", object.getObjectName().getCanonicalName());
    }

    @Test
    public void testJmxInvokeJava6UpBasic() throws MalformedObjectNameException {
        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        String name = "solr*:type=queryResultCache,*";
        JmxInvoke object = new JmxInvoke(name, factory.getSafeObjectName(name), "invokeit", new Object[0],
                new String[0]);
        Assert.assertEquals("solr*:type=queryResultCache,*", object.getObjectNameString());
        Assert.assertEquals("solr*:type=queryResultCache,*", factory.getSafeObjectName("solr*:type=queryResultCache,*"));
    }

    @Test
    public void testJmxInvokeJava6UpRegex() throws MalformedObjectNameException {
        JmxObjectFactory factory = JmxObjectFactory.createJmxFactory();
        String name = "solr*:type=queryResultCache, property=hello";
        JmxInvoke object = new JmxInvoke(name, factory.getSafeObjectName(name), "invokeit", new Object[0],
                new String[0]);
        Assert.assertEquals("solr*:type=queryResultCache, property=hello", object.getObjectNameString());
        Assert.assertEquals("solr*:type=queryResultCache, property=hello",
                factory.getSafeObjectName("solr*:type=queryResultCache, property=hello"));
        Assert.assertEquals("solr*: property=hello,type=queryResultCache", object.getObjectName().getCanonicalName());
    }

}
