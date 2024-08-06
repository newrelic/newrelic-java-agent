package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class SuperAgentIntegrationConfigImpl extends BaseConfig implements SuperAgentIntegrationConfig {
    public static final String ROOT = "superagent";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.superagent";
    public static final String FLEET_ID = "fleet_id";

    private final String fleetId;

    private final SuperAgentIntegrationHealthConfig superAgentIntegrationHealthConfig;

    public SuperAgentIntegrationConfigImpl(Map<String, Object> configProps) {
        super(configProps, SYSTEM_PROPERTY_ROOT);

        if (configProps == null) {
            configProps = Collections.emptyMap();
        }

        fleetId = getProperty(FLEET_ID);
        superAgentIntegrationHealthConfig = createHealthConfig();
    }

    private SuperAgentIntegrationHealthConfig createHealthConfig() {
        Map<String, Object> healthProps = getProperty(SuperAgentIntegrationHealthConfig.ROOT, Collections.emptyMap());
        return new SuperAgentIntegrationHealthConfig(healthProps, SYSTEM_PROPERTY_ROOT);
    }

    @Override
    public boolean isEnabled() {
        return fleetId != null;
    }

    @Override
    public String getHealthDeliveryLocation() {
        return superAgentIntegrationHealthConfig.getHealthDeliveryLocation();
    }

    @Override
    public int getHealthReportingFrequency() {
        return superAgentIntegrationHealthConfig.getHealthReportingFrequency();
    }

    @Override
    public String getFleetId() {
        return fleetId;
    }

    @Override
    public String getHealthClientType() {
        return superAgentIntegrationHealthConfig.getHealthClientType();
    }
}
