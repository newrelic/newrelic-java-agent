package com.newrelic.agent.config;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class SuperAgentIntegrationHealthConfig extends BaseConfig {
    public static final String ROOT = "health";
    public static final String FREQUENCY = "frequency";
    public static final int FREQUENCY_DEFAULT = 5;  // In seconds
    public static final String LOCATION = "delivery_location";  //URI Format; ex: file://opt/tmp/health.yml

    private final int frequency;;
    private final URI deliveryLocation;
    private final String healthClientType;

    public SuperAgentIntegrationHealthConfig(Map<String, Object> props, String systemPropertyPrefix) throws URISyntaxException {
        super(props, systemPropertyPrefix);
        frequency = getProperty(FREQUENCY, FREQUENCY_DEFAULT);

        // Location is in URI format; the client type is then derived from the URI scheme (file, http..)
        deliveryLocation = new URI(getProperty(LOCATION));
        healthClientType = deliveryLocation.getScheme();

        if (StringUtils.isAnyBlank(healthClientType, deliveryLocation.getPath(), deliveryLocation.getScheme())) {
            throw new URISyntaxException(getProperty(LOCATION), "Not a valid URI for the Super Agent integration service");
        }
    }

    public int getHealthReportingFrequency() {
        return frequency;
    }

    public URI getHealthDeliveryLocation() {
        return deliveryLocation;
    }

    public String getHealthClientType() {
        return healthClientType;
    }
}
