/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.environment;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;

public class EnvironmentTest {

    @Test
    public void overrideServerPort() throws Exception {
        System.setProperty("newrelic.config.appserver_port", "666");
        AgentConfig config = AgentConfigFactory.createAgentConfig(null, null, null);
        try {
            Environment env = new Environment(config, "c:\\test\\log");
            Assert.assertEquals(666, env.getAgentIdentity().getServerPort().intValue());
        } finally {
            System.clearProperty("newrelic.config.appserver_port");
        }
    }

    @Test
    public void overrideServerDispatcher() throws Exception {
        System.setProperty("newrelic.config.appserver_dispatcher", "Tomcat");
        AgentConfig config = AgentConfigFactory.createAgentConfig(null, null, null);
        try {
            Environment env = new Environment(config, "c:\\test\\log");
            Assert.assertEquals("Tomcat", env.getAgentIdentity().getDispatcher());
        } finally {
            System.clearProperty("newrelic.config.appserver_dispatcher");
        }
    }

    @Test
    public void testFixString() {
        Assert.assertEquals("test", Environment.fixString("test"));
        Assert.assertEquals("test\\dude", Environment.fixString("test\\dude"));
        Assert.assertEquals("test", Environment.fixString("test\\"));
        Assert.assertEquals("test", Environment.fixString("test\\\\"));
    }

    @Test
    public void testSetServerInfo() {
        AgentConfig config = AgentConfigFactory.createAgentConfig(null, null, null);

        final AtomicInteger changeCount = new AtomicInteger();
        EnvironmentChangeListener listener = new EnvironmentChangeListener() {

            @Override
            public void agentIdentityChanged(AgentIdentity identity) {
                changeCount.incrementAndGet();
            }
        };

        Environment env = new Environment(config, "c:\\test\\log");
        env.addEnvironmentChangeListener(listener);

        Assert.assertEquals("Unknown", env.getAgentIdentity().getDispatcher());
        Assert.assertFalse(env.getAgentIdentity().isServerInfoSet());

        env.setServerInfo("jetty", null);

        Assert.assertEquals("jetty", env.getAgentIdentity().getDispatcher());
        Assert.assertFalse(env.getAgentIdentity().isServerInfoSet());
        Assert.assertEquals(1, changeCount.get());

        env.setServerInfo("jetty2", "6.66");
        Assert.assertTrue(env.getAgentIdentity().isServerInfoSet());

        Assert.assertEquals(2, changeCount.get());

        Assert.assertEquals("jetty", env.getAgentIdentity().getDispatcher());
        Assert.assertEquals("6.66", env.getAgentIdentity().getDispatcherVersion());

        env.setServerInfo("ignore", "1.23");
        Assert.assertTrue(env.getAgentIdentity().isServerInfoSet());

        Assert.assertEquals(2, changeCount.get());

        Assert.assertEquals("jetty", env.getAgentIdentity().getDispatcher());
        Assert.assertEquals("6.66", env.getAgentIdentity().getDispatcherVersion());
    }

    @Test
    public void json() throws Exception {
        AgentConfig config = AgentConfigFactory.createAgentConfig(null, null, null);
        Environment env = new Environment(config, "c:\\test\\log");
        Object json = AgentHelper.serializeJSON(env);
        Assert.assertTrue(json.toString().indexOf("c:\\\\test\\\\log") > 0);
    }

    @Test
    public void testJvmProp() {
        // should not send JVM props by default
        String randomProp = "hello";
        System.setProperty(randomProp, "true");
        AgentConfig config = AgentConfigFactory.createAgentConfig(null, null, null);
        try {
            Environment env = new Environment(config, "c:\\test\\log");
            Assert.assertNull(env.getVariable("JVM arguments"));
        } finally {
            System.clearProperty(randomProp);
        }

        // setting to true will send the JVM props
        String property = "newrelic.config." + AgentConfigImpl.SEND_JVM_PROPS;
        System.setProperty(property, "true");
        config = AgentConfigFactory.createAgentConfig(null, null, null);
        try {
            Environment env = new Environment(config, "c:\\test\\log");
            Assert.assertNotNull(env.getVariable("JVM arguments"));
        } finally {
            System.clearProperty(property);
        }

    }
}
