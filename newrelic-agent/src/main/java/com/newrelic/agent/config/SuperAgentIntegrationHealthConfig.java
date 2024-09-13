package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;

public class SuperAgentIntegrationHealthConfig extends BaseConfig {
    public static final String ROOT = "health";
    public static final String FREQUENCY = "frequency";
    public static final int FREQUENCY_DEFAULT = 5;  // In seconds
    public static final String LOCATION = "delivery_location";  //URI Format; ex: file://opt/tmp/health.yml

    private final int frequency;;
    private URI deliveryLocation;
    private String healthClientType;

    public SuperAgentIntegrationHealthConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix);
        frequency = getProperty(FREQUENCY, FREQUENCY_DEFAULT);

        // Location is in URI format; the client type is then derived from the URI scheme (file, http..)
        validateAndAssignLocationUri(getProperty(LOCATION));
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

    private void validateAndAssignLocationUri(String locationAsUri) {
        if (StringUtils.isNotEmpty(locationAsUri)) {
            try {
                deliveryLocation = new URI(locationAsUri);
            } catch (Exception e) {
                Agent.LOG.log(Level.WARNING, "Invalid URI specified for health file delivery location: {0}", (String)getProperty(LOCATION));
                deliveryLocation = null;
                return;
            }

            healthClientType = deliveryLocation.getScheme();

            // Ensure the URI contains the scheme and path
            if (StringUtils.isAnyEmpty(healthClientType, deliveryLocation.getPath())) {
                deliveryLocation = null;
            }
        }

    }
}
