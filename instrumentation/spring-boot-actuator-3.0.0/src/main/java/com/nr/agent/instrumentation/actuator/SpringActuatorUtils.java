/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.actuator;

import com.newrelic.api.agent.NewRelic;

import java.util.List;

public class SpringActuatorUtils {
    private static final String [] TARGET_ACTUATOR_ENDPOINT_PREFIXES;

    static {
        String configuredPrefixes = NewRelic.getAgent().getConfig().getValue("class_transformer.spring_actuator_endpoint_prefixes", "actuator/health,actuator/loggers");
        TARGET_ACTUATOR_ENDPOINT_PREFIXES = configuredPrefixes == null ? new String[] {} : configuredPrefixes.split(",");
    }

    public static String normalizeActuatorUri(String uri) {
        if (uri != null && uri.startsWith("/")) {
            return uri.substring(1);
        }

        return uri;
    }

    /**
     * Check the actuator's endpoint URI and see if it matches one of the configured reportable prefixes.
     * If so, return that prefix as the String that will be part of the transaction name.
     *
     * @param endpointUri the actuator endpoint URI; "actuator/loggers/com.newrrelic" for example
     *
     * @return the String that should be reported as part of the transaction name; "actuator/loggers"
     * for example or null if the URI doesn't match and configured prefixes
     */
    public static String getReportableUriFromActuatorEndpoint(String endpointUri) {
        if (endpointUri != null) {
            for (String prefix : TARGET_ACTUATOR_ENDPOINT_PREFIXES) {
                if (endpointUri.startsWith(prefix)) {
                    return prefix;
                }
            }
        }

        return null;
    }
}
