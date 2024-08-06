package com.newrelic.agent.config;

import java.util.Map;

public class SuperAgentIntegrationHealthConfig extends BaseConfig {
    public static final String ROOT = "health";
    public static final String FREQUENCY = "frequency";
    public static final int FREQUENCY_DEFAULT = 5;  // In seconds
    public static final String LOCATION = "delivery_location";
    public static final String HEALTH_CLIENT_TYPE = "client_type";
    public static final String HEALTH_CLIENT_TYPE_DEFAULT = "File";


    private final int frequency;;
    private final String delivery_location;
    private final String healthClientType;

    public SuperAgentIntegrationHealthConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix);
        frequency = getProperty(FREQUENCY, FREQUENCY_DEFAULT);
        delivery_location = getProperty(LOCATION);
        healthClientType = getProperty(HEALTH_CLIENT_TYPE, HEALTH_CLIENT_TYPE_DEFAULT);
    }

    public int getHealthReportingFrequency() {
        return frequency;
    }

    public String getHealthDeliveryLocation() {
        return delivery_location;
    }

    public String getHealthClientType() {
        return healthClientType;
    }
}
