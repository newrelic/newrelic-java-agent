package com.newrelic.agent.config;

import java.util.Map;

public class SuperAgentIntegrationHealthConfig extends BaseConfig {
    public static final String ROOT = "health";

    public static final String FREQUENCY = "frequency";
    public static final int FREQUENCY_DEFAULT = 5;  // In seconds
    public static final String LOCATION = "delivery_location";

    private final int frequency;;
    private final String delivery_location;

    public SuperAgentIntegrationHealthConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix);
        frequency = getProperty(FREQUENCY, FREQUENCY_DEFAULT);
        delivery_location = getProperty(LOCATION);
    }

    public int getHealthReportingFrequency() {
        return frequency;
    }

    public String getHealthDeliveryLocation() {
        return delivery_location;
    }
}
