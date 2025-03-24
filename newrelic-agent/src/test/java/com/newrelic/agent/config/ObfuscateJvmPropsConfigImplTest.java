package com.newrelic.agent.config;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class ObfuscateJvmPropsConfigImplTest {

    ObfuscateJvmPropsConfig config;

    @After
    public void clearProps() {
        System.clearProperty("newrelic.config.obfuscate_jvm_props.enabled");
        System.clearProperty("newrelic.config.obfuscate_jvm_props.allow");
        System.clearProperty("newrelic.config.obfuscate_jvm_props.block");
    }

    @Test
    public void isEnabledShouldDefaultToTrue() {
        config = new ObfuscateJvmPropsConfigImpl(null);
        Assert.assertTrue(config.isEnabled());
    }

    @Test
    public void isEnabledCanBeSetToFalse() {
        System.setProperty("newrelic.config.obfuscate_jvm_props.enabled", "false");
        config = new ObfuscateJvmPropsConfigImpl(null);
        Assert.assertFalse(config.isEnabled());
    }

    @Test
    public void testDefaultAllowIncludesXProps() {
        config = new ObfuscateJvmPropsConfigImpl(null);
        Set<String> allow = config.getAllow();
        Assert.assertTrue(allow.contains("-X*"));
    }

    @Test
    public void testGetAllow() {
        System.setProperty("newrelic.config.obfuscate_jvm_props.allow", "-Xprop*, -Dprop.two, -XX:propthree");
        config = new ObfuscateJvmPropsConfigImpl(null);
        Set<String> allow = config.getAllow();
        Assert.assertTrue(allow.contains("-Xprop*"));
        Assert.assertTrue(allow.contains("-Dprop.two"));
        Assert.assertTrue(allow.contains("-XX:propthree"));
    }

    @Test
    public void testDefaultBlockIsEmpty() {
        config = new ObfuscateJvmPropsConfigImpl(null);
        Set<String> block = config.getBlock();
        Assert.assertTrue(block.isEmpty());
    }

    @Test
    public void testBlock() {
        System.setProperty("newrelic.config.obfuscate_jvm_props.block", "-Dblock.me, -Xand.me.too*");
        config = new ObfuscateJvmPropsConfigImpl(null);
        Set<String> block = config.getBlock();
        Assert.assertTrue(block.contains("-Dblock.me"));
        Assert.assertTrue(block.contains("-Xand.me.too*"));
    }
}