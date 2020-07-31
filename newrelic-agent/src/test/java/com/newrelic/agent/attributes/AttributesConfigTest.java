/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AttributesConfigImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributesConfigTest {

    public AgentConfig createNonRootConfig(String type,
            Boolean enabledDest,
            List<String> attsInclude,
            List<String> attsExclude) {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> typeConfig = new HashMap<>();
        config.put(type, typeConfig);

        Map<String, Object> attsConfig = new HashMap<>();
        typeConfig.put(AgentConfigImpl.ATTRIBUTES, attsConfig);

        attsConfig.put(AttributesConfigImpl.INCLUDE, attsInclude);
        attsConfig.put(AttributesConfigImpl.EXCLUDE, attsExclude);
        
        if(enabledDest != null) {
            attsConfig.put(AttributesConfigImpl.ENABLED, enabledDest);
        }

        return AgentConfigImpl.createAgentConfig(config);
    }

    public AgentConfig createRootConfig(String type,
            Boolean enabledRoot,
            Boolean enabledDest,
            List<String> attsInclude,
            List<String> attsExclude) {

        Map<String, Object> config = new HashMap<>();
        Map<String, Object> rootAtts = new HashMap<>();
        config.put(AgentConfigImpl.ATTRIBUTES, rootAtts);
        rootAtts.put("enabled", enabledRoot);

        Map<String, Object> typeConfig = new HashMap<>();
        config.put(type, typeConfig);

        Map<String, Object> attsConfig = new HashMap<>();
        typeConfig.put(AgentConfigImpl.ATTRIBUTES, attsConfig);

        attsConfig.put(AttributesConfigImpl.INCLUDE, attsInclude);
        attsConfig.put(AttributesConfigImpl.EXCLUDE, attsExclude);

        if(enabledDest != null) {
            attsConfig.put(AttributesConfigImpl.ENABLED, enabledDest);
        }

        return AgentConfigImpl.createAgentConfig(config);
    }

    @Test
    public void testEnableAttsError() {
        testEnabledAttributes(AgentConfigImpl.ERROR_COLLECTOR);
    }

    @Test
    public void testEnableAttsBrowser() {
        testEnabledAttributes(AgentConfigImpl.BROWSER_MONITORING);
    }

    @Test
    public void testEnableAttsTracers() {
        testEnabledAttributes(AgentConfigImpl.TRANSACTION_TRACER);
    }

    @Test
    public void testEnableAttsEvents() {
        testEnabledAttributes(AgentConfigImpl.TRANSACTION_EVENTS);
    }

    public void testEnabledAttributes(String type) {
        AgentConfig config = createNonRootConfig(type, false, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, type));

        // set to true
        config = createNonRootConfig(type, true,Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, false, type));

        // and test the true default
        config = createNonRootConfig(type, null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, true, type));

        // and test the false default
        config = createNonRootConfig(type, null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, false, type));
    }

    @Test
    public void testIsAttsEnabledInvalidValue() {

        String type = AgentConfigImpl.TRANSACTION_EVENTS;
        AgentConfig config = createNonRootConfig(type, false, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, AgentConfigImpl.TRANSACTION_EVENTS));

        HashMap<String, Object> settings = new HashMap<>();
        HashMap<String, Object> localSettings = new HashMap<>();
        settings.put(AgentConfigImpl.TRANSACTION_EVENTS, localSettings);
        HashMap<String, Object> attributesMap = new HashMap<>();
        localSettings.put(AgentConfigImpl.ATTRIBUTES, attributesMap);
        attributesMap.put("enabled", "blabla");
        config = AgentConfigImpl.createAgentConfig(settings);
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, AgentConfigImpl.TRANSACTION_EVENTS));
        
        config = createNonRootConfig(type, true, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, true, AgentConfigImpl.TRANSACTION_EVENTS));

        settings = new HashMap<>();
        localSettings = new HashMap<>();
        settings.put(AgentConfigImpl.TRANSACTION_EVENTS, localSettings);
        attributesMap = new HashMap<>();
        localSettings.put(AgentConfigImpl.ATTRIBUTES, attributesMap);
        attributesMap.put("enabled", "TRUE");
        config = AgentConfigImpl.createAgentConfig(settings);
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, true, AgentConfigImpl.TRANSACTION_EVENTS));

    }

    @Test
    public void testEnableAttsWithCaptureError() {
        testCaptureAttsMergingBrowser(AgentConfigImpl.ERROR_COLLECTOR);
    }

    @Test
    public void testEnableAttsWithCaptureBrowser() {
        testCaptureAttsMergingBrowser(AgentConfigImpl.BROWSER_MONITORING);
    }

    @Test
    public void testEnableAttsWitCaptureTracers() {
        testCaptureAttsMergingBrowser(AgentConfigImpl.TRANSACTION_TRACER);
    }

    @Test
    public void testEnableAttsWitCaptureAnalytics() {
        testCaptureAttsMergingBrowser(AgentConfigImpl.TRANSACTION_EVENTS);
    }

    public void testCaptureAttsMergingBrowser(String type) {
        AgentConfig config = createNonRootConfig(type, false, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, type));

        // only capture atts
        config = createNonRootConfig(type, null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, true, type));

        // only capture atts
        config = createNonRootConfig(type, null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, false, type));
    }

    @Test
    public void testEnabledAllLevelsError() {
        testEnabledAllLevels(AgentConfigImpl.ERROR_COLLECTOR);
    }

    @Test
    public void testEnabledAllLevelsBrowser() {
        testEnabledAllLevels(AgentConfigImpl.BROWSER_MONITORING);
    }

    @Test
    public void testEnabledAllLevelsTracers() {
        testEnabledAllLevels(AgentConfigImpl.TRANSACTION_TRACER);
    }

    @Test
    public void testEnabledAllLevelsEvents() {
        testEnabledAllLevels(AgentConfigImpl.TRANSACTION_EVENTS);
    }

    public void testEnabledAllLevels(String type) {
        AgentConfig config = createRootConfig(type, true,false, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, type));
        
        config = createRootConfig(type, false,true, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, type));

        config = createRootConfig(type, true,true, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, false, type));
        
        config = createRootConfig(type, false,false, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, type));
        
        config = createRootConfig(type, true,null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, false, type));
        
        config = createRootConfig(type, true,null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, true, type));

        config = createRootConfig(type, null,false, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, true, type));

        config = createRootConfig(type, null,false, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, false, type));

        config = createRootConfig(type, null,true, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, true, type));

        config = createRootConfig(type, null,true, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, false, type));

        // the defaults
        config = createRootConfig(type, null,null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(false, config.getAttributesConfig().isAttsEnabled(config, false, type));

        config = createRootConfig(type, null,null, Collections.<String>emptyList(), Collections.<String>emptyList());
        Assert.assertEquals(true, config.getAttributesConfig().isAttsEnabled(config, true, type));
    }

}
