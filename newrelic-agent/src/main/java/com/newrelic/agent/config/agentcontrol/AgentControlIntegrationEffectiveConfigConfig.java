package com.newrelic.agent.config.agentcontrol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class AgentControlIntegrationEffectiveConfigConfig extends AgentControlIntegrationBaseConfig {
    public static final String ROOT = "effective_config";

    public static final String LOCATION = "delivery_location";  //URI Format; ex: file://opt/tmp
    public static final URI LOCATION_DEFAULT;

    private DeliveryLocation deliveryLocation;

    static {
        URI tmpLocationDefault = null;
        try {
            tmpLocationDefault = new URI ("file:///newrelic/apm/effective_config");
        } catch (URISyntaxException ignored) {
            // Can never happen
        }
        LOCATION_DEFAULT = tmpLocationDefault;
    }

    public AgentControlIntegrationEffectiveConfigConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix + ROOT + ".");

        // Location is in URI format; the client type is then derived from the URI scheme (file, http..)
        deliveryLocation = validateAndAssignDeliveryLocation(getProperty(LOCATION), LOCATION_DEFAULT);
    }

    public URI getEffectiveConfigDeliveryLocation() {
        return deliveryLocation.getDeliveryLocation();
    }

    public String getEffectiveConfigClientType() {
        return deliveryLocation.getClientType();
    }
}
