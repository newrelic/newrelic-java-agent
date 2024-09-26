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
    public static final boolean isActuatorEndpointNamingEnabled = NewRelic.getAgent().getConfig().getValue("class_transformer.name_actuator_endpoints", false);

    public static String normalizeActuatorUri(String uri) {
        String modifiedUri = null;
        if (uri != null) {
            // Normalize the uri by removing the leading "/" and stripping and path components
            // other than the first two, to prevent MGI for certain actuator endpoints.
            // For example, "/actuator/loggers/com.newrelic" will be converted into
            // "actuator/loggers"
            String [] parts = uri.replaceFirst("^/", "").split("/");
            if (parts.length >= 2) {
                 modifiedUri = parts[0] + "/" + parts[1];
            }
        }

        return modifiedUri;
    }
}
