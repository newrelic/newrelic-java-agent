/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.InsightsConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class CustomInsightsEventsConfigUtilsTest {

    @Test
    public void testMaxSamplesStores() {
        // setup
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> customEvents = new HashMap<>();
        root.put("custom_insights_events", customEvents);

        // start with the default
        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertEquals(InsightsConfigImpl.DEFAULT_MAX_SAMPLES_STORED,
                config.getInsightsConfig().getMaxSamplesStored());

        // under config
        customEvents.clear();
        customEvents.put(InsightsConfigImpl.MAX_SAMPLES_STORED_PROP, 10);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertEquals(10, config.getInsightsConfig().getMaxSamplesStored());

        // over config
        customEvents.clear();
        customEvents.put(InsightsConfigImpl.MAX_SAMPLES_STORED_PROP, 100000000);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertEquals(100000000, config.getInsightsConfig().getMaxSamplesStored());
    }

    @Test
    public void testEnabled() {
        // setup
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> customEvents = new HashMap<>();
        root.put("custom_insights_events", customEvents);
        customEvents.put(InsightsConfigImpl.MAX_SAMPLES_STORED_PROP, 10);
        MockServiceManager mockServiceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(mockServiceManager);
        mockServiceManager.setConfigService(Mockito.mock(ConfigService.class));

        // start with the default
        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertTrue(config.getInsightsConfig().isEnabled());

        // high security enabled
        root.put(AgentConfigImpl.HIGH_SECURITY, true);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertFalse(config.getInsightsConfig().isEnabled());

        // high security disabled
        root.put(AgentConfigImpl.HIGH_SECURITY, false);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertTrue(config.getInsightsConfig().isEnabled());

        // no samples
        customEvents.put(InsightsConfigImpl.MAX_SAMPLES_STORED_PROP, 0);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertFalse(config.getInsightsConfig().isEnabled());
        customEvents.put(InsightsConfigImpl.MAX_SAMPLES_STORED_PROP, 10);

        // go through both properties
        customEvents.clear();
        customEvents.put(InsightsConfigImpl.ENABLED_PROP, false);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertFalse(config.getInsightsConfig().isEnabled());

        // Transaction Events feature gate should have no effect.
        customEvents.clear();
        customEvents.put("collect_analytics_events", false);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertTrue(config.getInsightsConfig().isEnabled());

        // Test Custom Events feature gate:
        customEvents.clear();
        customEvents.put("collect_custom_events", false);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertFalse(config.getInsightsConfig().isEnabled());

        // set both
        customEvents.clear();
        customEvents.put(InsightsConfigImpl.ENABLED_PROP, true);
        customEvents.put("collect_custom_events", false);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertFalse(config.getInsightsConfig().isEnabled());

        customEvents.clear();
        customEvents.put(InsightsConfigImpl.ENABLED_PROP, false);
        customEvents.put("collect_custom_events", true);
        config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);
        Assert.assertFalse(config.getInsightsConfig().isEnabled());
    }

    @Test
    public void testMaxAttributeValue() {
        // setup
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> customEvents = new HashMap<>();
        root.put("custom_insights_events", customEvents);

        // start with the default
        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertEquals(InsightsConfigImpl.DEFAULT_MAX_ATTRIBUTE_VALUE,
                config.getInsightsConfig().getMaxAttributeValue());

        // under config
        customEvents.clear();
        customEvents.put(InsightsConfigImpl.MAX_ATTRIBUTE_VALUE, 10);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertEquals(10, config.getInsightsConfig().getMaxAttributeValue());

        // over config
        customEvents.clear();
        customEvents.put(InsightsConfigImpl.MAX_ATTRIBUTE_VALUE, 100000000);
        config = AgentConfigImpl.createAgentConfig(root);
        Assert.assertEquals(InsightsConfigImpl.MAX_MAX_ATTRIBUTE_VALUE, config.getInsightsConfig().getMaxAttributeValue());
    }
}
