/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class AgentControlIntegrationConfigImpl extends BaseConfig implements AgentControlIntegrationConfig {
    public static final String ROOT = "agent_control";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.agent_control.";
    public static final String ENABLED = "enabled";

    private boolean enabled;
    public static final boolean ENABLED_DEFAULT = false;


    private AgentControlIntegrationHealthConfig agentControlIntegrationHealthConfig;

    public AgentControlIntegrationConfigImpl(Map<String, Object> configProps) {
        super(configProps, SYSTEM_PROPERTY_ROOT);
        enabled = getProperty(ENABLED, ENABLED_DEFAULT);

        if (enabled) {
            agentControlIntegrationHealthConfig = createHealthConfig();
            if (agentControlIntegrationHealthConfig.getHealthDeliveryLocation() == null) {
                Agent.LOG.log(Level.WARNING, "Configured Agent Control health delivery location is not a valid URI; " +
                        "Agent Control integration service will not be started");
                enabled = false;
            }
        }
    }

    private AgentControlIntegrationHealthConfig createHealthConfig() {
        Map<String, Object> healthProps = getProperty(AgentControlIntegrationHealthConfig.ROOT, Collections.emptyMap());
        return new AgentControlIntegrationHealthConfig(healthProps, SYSTEM_PROPERTY_ROOT);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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
    public String getHealthClientType() {
        return agentControlIntegrationHealthConfig == null ? null : agentControlIntegrationHealthConfig.getHealthClientType();
    }
}
