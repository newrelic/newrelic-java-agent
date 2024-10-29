/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.environment;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
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
        // the default should be false
        String randomProp = "hello";
        System.setProperty(randomProp, "true");
        AgentConfig config = AgentConfigFactory.createAgentConfig(null, null, null);
        try {
            Environment env = new Environment(config, "c:\\test\\log");
            Assert.assertNotNull(env.getVariable("JVM arguments"));
        } finally {
            System.clearProperty(randomProp);
        }

        // but you can set it to true
        String property = "newrelic.config." + AgentConfigImpl.SEND_JVM_PROPS;
        System.setProperty(property, "false");
        config = AgentConfigFactory.createAgentConfig(null, null, null);
        try {
            Environment env = new Environment(config, "c:\\test\\log");
            Assert.assertNull(env.getVariable("JVM arguments"));
        } finally {
            System.clearProperty(property);
        }
    }

    //bunch of jvm props tests

    @Test
    public void obfuscateJvmProps_defaultIsTrue() {
        List<String> props = exampleJvmProps();
        List<String> obfuscatedProps = getObfuscatedProps(props);
        //nothing has been configured, all values should be obfuscated
        Assert.assertEquals(props.size(), obfuscatedProps.size());
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.A=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B.extended=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.C=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-javaagent:1234562"));
    }

    @Test
    public void obfuscateJvmProps_doesNotObfuscateWhenDisabled() {
        System.setProperty("newrelic.config.obfuscate_jvm_props.enabled", "false");
        List<String> props = exampleJvmProps();
        List<String> obfuscatedProps = getObfuscatedProps(props);

        Assert.assertEquals(props.size(), obfuscatedProps.size());
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.A=one"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B=two"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B.extended=three"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.C=four"));
        Assert.assertTrue(obfuscatedProps.contains("-javaagent:1234562"));
    }

    @Test
    public void obfuscateJvmProps_doesNotTouchAgentFlag() {
        //the current implementation does not modify the -javaagent prop.
        //if the implementation changes we want to make sure the agent flag is still sent unmodified!
        List<String> props = new ArrayList<>();
        props.add("-javaagent:1234562abcdefg");
        List<String> obfuscatedProps = getObfuscatedProps(props);
        Assert.assertEquals(props.size(), obfuscatedProps.size());
        Assert.assertTrue(obfuscatedProps.contains("-javaagent:1234562abcdefg"));
    }

    @Test
    public void obfuscateJvmProps_allowAndBlock() {
        System.setProperty("newrelic.config.obfuscate_jvm_props.allow", "-Dprop.A, -Dprop.B*");
        List<String> props = exampleJvmProps();
        List<String> obfuscatedProps = getObfuscatedProps(props);

        Assert.assertEquals(props.size(), obfuscatedProps.size());
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.A=one"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B=two"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B.extended=three"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.C=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-javaagent:1234562"));
    }

    @Test
    public void obfuscateJvmProps_specificityAlwaysWins() {
        //when there is overlap, the more specific rule should always apply
        //ie propA.extended and propB.extended rules apply regardless of which list they belong to
        System.setProperty("newrelic.config.obfuscate_jvm_props.allow", "-Dprop.A.extended, -Dprop.B*");
        System.setProperty("newrelic.config.obfuscate_jvm_props.block", "-Dprop.A*, -Dprop.B.extended");
        List<String> props = exampleJvmProps();
        props.add("-Dprop.A.extended=six");
        List<String> obfuscatedProps = getObfuscatedProps(props);

        Assert.assertEquals(props.size(), obfuscatedProps.size());
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.A=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.A.extended=six"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B=two"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B.extended=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.C=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-javaagent:1234562"));
    }

    @Test
    public void obfuscateJvmProps_blockWins() {
        System.setProperty("newrelic.config.obfuscate_jvm_props.allow", "-Dprop.A, -Dprop.B*");
        System.setProperty("newrelic.config.obfuscate_jvm_props.block", "-Dprop.A");
        List<String> props = exampleJvmProps();
        List<String> obfuscatedProps = getObfuscatedProps(props);

        Assert.assertEquals(props.size(), obfuscatedProps.size());
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.A=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B=two"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.B.extended=three"));
        Assert.assertTrue(obfuscatedProps.contains("-Dprop.C=obfuscated"));
        Assert.assertTrue(obfuscatedProps.contains("-javaagent:1234562"));
    }

    private List<String> exampleJvmProps() {
        List<String> props = new ArrayList<>();
        props.add("-Dprop.A=one");
        props.add("-Dprop.B=two");
        props.add("-Dprop.B.extended=three");
        props.add("-Dprop.C=four");
        props.add("-javaagent:1234562");
        return props;
    }

    private List<String> getObfuscatedProps(List<String> props) {
        AgentConfig config = AgentConfigFactory.createAgentConfig(null, null, null);
        Environment env = new Environment(config, "c:\\test\\log");
        return env.obfuscateProps(props, config.getObfuscateJvmPropsConfig());
    }

    @After
    public void clearObfuscateJvmPropsSettings() {
        System.clearProperty("newrelic.config.obfuscate_jvm_props.enabled");
        System.clearProperty("newrelic.config.obfuscate_jvm_props.allow");
        System.clearProperty("newrelic.config.obfuscate_jvm_props.block");
    }
}
