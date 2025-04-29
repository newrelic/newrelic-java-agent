/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class AgentControlIntegrationHealthConfig extends BaseConfig {
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
    private URI deliveryLocation;
    private String healthClientType;

    public AgentControlIntegrationHealthConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix + ROOT + ".");
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
                deliveryLocation = null;
                return;
            }
        } else {
            deliveryLocation = LOCATION_DEFAULT;
        }

        healthClientType = deliveryLocation.getScheme();

        // Ensure the URI contains the scheme and path
        if (StringUtils.isAnyEmpty(healthClientType, deliveryLocation.getPath())) {
            deliveryLocation = null;
        }
    }
}
