/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;

public class LicenseKeyUtil {
    private static final String OBFUSCATED_LICENSE_KEY = "obfuscated";

    /**
     * Removes the license_key value from a given string.
     * <p>
     * This is primarily used to prevent the license_key from being
     * written to the agent logs when using debug and/or audit_mode logging.
     *
     * @param originalString String to be evaluated and obfuscated if it contains the license_key
     * @return A modified String with the license_key value replaced, if it exists. Otherwise, the originalString is returned.
     */
    public static String obfuscateLicenseKey(String originalString) {
        if (originalString == null || originalString.isEmpty()) {
            Agent.LOG.finest("Unable to obfuscate the license_key in a null or empty string.");
            return originalString;
        }
        String licenseKey = ServiceFactory.getConfigService().getDefaultAgentConfig().getLicenseKey();
        if (licenseKey == null) {
            Agent.LOG.finest("Unable to obfuscate a null license_key.");
            return originalString;
        } else {
            return originalString.replace(licenseKey, OBFUSCATED_LICENSE_KEY);
        }
    }
}
