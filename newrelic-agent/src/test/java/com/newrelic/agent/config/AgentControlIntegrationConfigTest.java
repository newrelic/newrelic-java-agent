/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AgentControlIntegrationConfigTest {
    @Test
    public void agentControlConfig_withValidProperties_createsValidConfig() {
        Map<String, Object> agentControlConfigProps = new HashMap<>();
        Map<String, Object> healthConfigProps = new HashMap<>();
        agentControlConfigProps.put("fleet_id", "12345");

        healthConfigProps.put("delivery_location", "file:///foo/bar");
        healthConfigProps.put("frequency", 5);
        agentControlConfigProps.put("health", healthConfigProps);

        AgentControlIntegrationConfig config = new AgentControlIntegrationConfigImpl(agentControlConfigProps);
        assertEquals("12345", config.getFleetId());
        assertEquals(5, config.getHealthReportingFrequency());
        assertEquals("file", config.getHealthClientType());
        assertEquals("/foo/bar", config.getHealthDeliveryLocation().getPath());
    }

    @Test
    public void agentControlConfig_withInvalidLocation_nullsFleetId() {
        Map<String, Object> agentControlConfigProps = new HashMap<>();
        Map<String, Object> healthConfigProps = new HashMap<>();
        agentControlConfigProps.put("fleet_id", "12345");

        healthConfigProps.put("delivery_location", "");
        agentControlConfigProps.put("health", healthConfigProps);

        AgentControlIntegrationConfig config = new AgentControlIntegrationConfigImpl(agentControlConfigProps);
        assertNull(config.getFleetId());
    }
}
