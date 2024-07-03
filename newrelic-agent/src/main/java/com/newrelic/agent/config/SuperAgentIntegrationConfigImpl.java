package com.newrelic.agent.config;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

public class SuperAgentIntegrationConfigImpl extends BaseConfig implements SuperAgentIntegrationConfig {
    public static final String ROOT = "health";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.health";
    public static final String ENDPOINT = "endpoint";
    public static final String FREQUENCY = "frequency";
    public static final int FREQUENCY_DEFAULT = 5;  // In seconds

    public SuperAgentIntegrationConfigImpl(Map<String, Object> configProps) {
        super(configProps, SYSTEM_PROPERTY_ROOT);
    }

    static SuperAgentIntegrationConfigImpl createSuperAgentConfig(Map<String, Object> configProps) {
        if (configProps == null) {
            configProps = Collections.emptyMap();
        }

        return new SuperAgentIntegrationConfigImpl(configProps);
    }

    @Override
    public boolean isEnabled() {
        return StringUtils.isNotEmpty(getProperty(ENDPOINT));
    }

    @Override
    public String getEndpoint() {
        return getProperty(ENDPOINT);
    }

    @Override
    public int getFrequency() {
        return getProperty(FREQUENCY, FREQUENCY_DEFAULT);
    }
}
