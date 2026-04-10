/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfigImpl;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AgentControlIntegrationConfigTest {
    @Test
    public void agentControlConfig_withValidProperties_createsValidConfig() {
        Map<String, Object> agentControlConfigProps = new HashMap<>();
        Map<String, Object> healthConfigProps = new HashMap<>();
        Map<String, Object> effectiveConfigConfigProps = new HashMap<>();

        agentControlConfigProps.put("enabled", true);
        healthConfigProps.put("delivery_location", "file:///foo/bar");
        healthConfigProps.put("frequency", 5);
        agentControlConfigProps.put("health", healthConfigProps);

        effectiveConfigConfigProps.put("delivery_location", "file:///foo/bar2");
        agentControlConfigProps.put("effective_config", effectiveConfigConfigProps);

        AgentControlIntegrationConfig config = new AgentControlIntegrationConfigImpl(agentControlConfigProps);
        assertTrue(config.isEnabled());
        assertEquals(5, config.getHealthReportingFrequency());
        assertEquals("file", config.getHealthClientType());
        assertEquals("/foo/bar", config.getHealthDeliveryLocation().getPath());
        assertEquals("file", config.getEffectiveConfigClientType());
        assertEquals("/foo/bar2", config.getEffectiveConfigDeliveryLocation().getPath());

    }

    @Test
    public void agentControlConfig_withEmptyLocation_usesDefault() {
        Map<String, Object> agentControlConfigProps = new HashMap<>();
        Map<String, Object> healthConfigProps = new HashMap<>();
        Map<String, Object> effectiveConfigConfigProps = new HashMap<>();

        agentControlConfigProps.put("enabled", true);
        healthConfigProps.put("delivery_location", "");
        effectiveConfigConfigProps.put("delivery_location", "");
        agentControlConfigProps.put("health", healthConfigProps);
        agentControlConfigProps.put("effective_config", effectiveConfigConfigProps);

        AgentControlIntegrationConfig config = new AgentControlIntegrationConfigImpl(agentControlConfigProps);
        assertEquals("file", config.getHealthClientType());
        assertEquals("/newrelic/apm/health", config.getHealthDeliveryLocation().getPath());
        assertEquals("file", config.getEffectiveConfigClientType());
        assertEquals("/newrelic/apm/effective_config", config.getEffectiveConfigDeliveryLocation().getPath());
        assertTrue(config.isEnabled());
    }

    @Test
    public void agentControlConfig_withInvalidUri_returnNull() {
        Map<String, Object> agentControlConfigProps = new HashMap<>();
        Map<String, Object> healthConfigProps = new HashMap<>();

        agentControlConfigProps.put("enabled", true);
        healthConfigProps.put("delivery_location", "file:///example.com/file[1].txt");
        agentControlConfigProps.put("health", healthConfigProps);

        AgentControlIntegrationConfig config = new AgentControlIntegrationConfigImpl(agentControlConfigProps);
        assertNull(config.getHealthDeliveryLocation());
        assertFalse(config.isEnabled());    //Since delivery location was invalid we flip enabled to false
    }
}
