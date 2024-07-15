package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class SuperAgentIntegrationConfigImpl extends BaseConfig implements SuperAgentIntegrationConfig {
    public static final String ROOT = "superagent";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.superagent";

    public static final String ENABLED = "enabled";
    public static final boolean ENABLED_DEFAULT = false;
    private final boolean enabled;

    private final SuperAgentIntegrationHealthConfig superAgentIntegrationHealthConfig;

    public SuperAgentIntegrationConfigImpl(Map<String, Object> configProps) {
        super(configProps, SYSTEM_PROPERTY_ROOT);

        if (configProps == null) {
            configProps = Collections.emptyMap();
        }
        enabled = getProperty(ENABLED, ENABLED_DEFAULT);
        superAgentIntegrationHealthConfig = createHealthConfig();
    }

    private SuperAgentIntegrationHealthConfig createHealthConfig() {
        Map<String, Object> healthProps = getProperty(SuperAgentIntegrationHealthConfig.ROOT, Collections.emptyMap());
        return new SuperAgentIntegrationHealthConfig(healthProps, SYSTEM_PROPERTY_ROOT);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getHealthDeliveryLocation() {
        return superAgentIntegrationHealthConfig.getHealthDeliveryLocation();
    }

    @Override
    public int getHealthReportingFrequency() {
        return superAgentIntegrationHealthConfig.getHealthReportingFrequency();
    }
}
