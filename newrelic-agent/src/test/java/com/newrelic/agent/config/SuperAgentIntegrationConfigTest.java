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

public class SuperAgentIntegrationConfigTest {
    @Test
    public void superAgentConfig_withValidProperties_createsValidConfig() {
        Map<String, Object> superAgentConfigProps = new HashMap<>();
        Map<String, Object> healthConfigProps = new HashMap<>();
        superAgentConfigProps.put("fleet_id", "12345");

        healthConfigProps.put("delivery_location", "file:///foo/bar");
        healthConfigProps.put("frequency", 5);
        superAgentConfigProps.put("health", healthConfigProps);

        SuperAgentIntegrationConfig config = new SuperAgentIntegrationConfigImpl(superAgentConfigProps);
        assertEquals("12345", config.getFleetId());
        assertEquals(5, config.getHealthReportingFrequency());
        assertEquals("file", config.getHealthClientType());
        assertEquals("/foo/bar", config.getHealthDeliveryLocation().getPath());
    }

    @Test
    public void superAgentConfig_withInvalidLocation_nullsFleetId() {
        Map<String, Object> superAgentConfigProps = new HashMap<>();
        Map<String, Object> healthConfigProps = new HashMap<>();
        superAgentConfigProps.put("fleet_id", "12345");

        healthConfigProps.put("delivery_location", "");
        superAgentConfigProps.put("health", healthConfigProps);

        SuperAgentIntegrationConfig config = new SuperAgentIntegrationConfigImpl(superAgentConfigProps);
        assertNull(config.getFleetId());
    }
}
