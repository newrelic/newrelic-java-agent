/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config.agentcontrol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class AgentControlIntegrationHealthConfig extends AgentControlIntegrationBaseConfig {
    public static final String ROOT = "health";
    public static final String FREQUENCY = "frequency";
    public static final int FREQUENCY_DEFAULT = 5;  // In seconds

    public static final String LOCATION = "delivery_location";  //URI Format; ex: file://opt/tmp
    public static final URI LOCATION_DEFAULT;

    static {
        URI tmpLocationDefault = null;
        try {
            tmpLocationDefault = new URI ("file:///newrelic/apm/health");
        } catch (URISyntaxException ignored) {
            // Can never happen
        }
        LOCATION_DEFAULT = tmpLocationDefault;
    }

    private final int frequency;
    private DeliveryLocation deliveryLocation;

    public AgentControlIntegrationHealthConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix + ROOT + ".");
        frequency = getProperty(FREQUENCY, FREQUENCY_DEFAULT);

        // Location is in URI format; the client type is then derived from the URI scheme (file, http..)
        deliveryLocation = validateAndAssignDeliveryLocation(getProperty(LOCATION), LOCATION_DEFAULT);
    }

    public int getHealthReportingFrequency() {
        return frequency;
    }

    public URI getHealthDeliveryLocation() {
        return deliveryLocation.getDeliveryLocation();
    }

    public String getHealthClientType() {
        return deliveryLocation.getClientType();
    }
}
