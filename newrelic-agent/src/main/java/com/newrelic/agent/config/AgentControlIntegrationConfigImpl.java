/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class AgentControlIntegrationConfigImpl extends BaseConfig implements AgentControlIntegrationConfig {
    public static final String ROOT = "agent_control";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.agent_control.";
    public static final String FLEET_ID = "fleet_id";

    private final String fleetId;

    private final AgentControlIntegrationHealthConfig agentControlIntegrationHealthConfig;

    public AgentControlIntegrationConfigImpl(Map<String, Object> configProps) {
        super(configProps, SYSTEM_PROPERTY_ROOT);
        agentControlIntegrationHealthConfig = createHealthConfig();
        String tmpFleetId = getProperty(FLEET_ID);

        if (StringUtils.isNotEmpty(tmpFleetId) && agentControlIntegrationHealthConfig.getHealthDeliveryLocation() == null) {
            Agent.LOG.log(Level.WARNING, "Configured Super Agent health delivery location is not a valid URI; " +
                    "Agent Control integration service will not be started");
            fleetId = null;
        } else {
            fleetId = tmpFleetId;
        }
    }

    private AgentControlIntegrationHealthConfig createHealthConfig() {
        Map<String, Object> healthProps = getProperty(AgentControlIntegrationHealthConfig.ROOT, Collections.emptyMap());
        AgentControlIntegrationHealthConfig agentControlIntegrationHealthConfig;

        agentControlIntegrationHealthConfig = new AgentControlIntegrationHealthConfig(healthProps, SYSTEM_PROPERTY_ROOT);
        return agentControlIntegrationHealthConfig;
    }

    @Override
    public boolean isEnabled() {
        return fleetId != null;
    }

    @Override
    public URI getHealthDeliveryLocation() {
        return agentControlIntegrationHealthConfig == null ? null : agentControlIntegrationHealthConfig.getHealthDeliveryLocation();
    }

    @Override
    public int getHealthReportingFrequency() {
        return agentControlIntegrationHealthConfig == null ? 0 : agentControlIntegrationHealthConfig.getHealthReportingFrequency();
    }

    @Override
    public String getFleetId() {
        return fleetId;
    }

    @Override
    public String getHealthClientType() {
        return agentControlIntegrationHealthConfig == null ? null : agentControlIntegrationHealthConfig.getHealthClientType();
    }
}
