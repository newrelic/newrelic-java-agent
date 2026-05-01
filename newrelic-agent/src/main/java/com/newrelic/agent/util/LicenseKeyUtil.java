/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseKeyUtil {
    private static final Pattern LICENSE_KEY_PATTERN = Pattern.compile("(.+?license_key(?:=|\":\"))([^&\"]+)");
    private static final int KEY_LENGTH_CUTOFF = 10;

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
            String obfuscatedKey = partialObfuscation(licenseKey);
            Matcher matcher = LICENSE_KEY_PATTERN.matcher(originalString);
            if (!matcher.find()) {
                return originalString;
            }

            // It's not clear this can happen in the real world, but there was a test against
            // a String with multiple "license_key=" fields. This loop simply iterates over the matcher
            // and does the replacement for all matches.
            StringBuffer sb = new StringBuffer();
            do {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + obfuscatedKey));
            } while (matcher.find());
            matcher.appendTail(sb);

            return sb.toString();
        }
    }

    private static String partialObfuscation(String licenseKey) {
        // Null / empty check has already occurred
        int keyLength =  licenseKey.length();
        String obfuscatedKey;

        // Per the spec: If length is <= 10, replace the full key with "*"
        // Otherwise, keep the first 10 characters of the key and replace the
        // remaining key characters with "*"
        if (keyLength > KEY_LENGTH_CUTOFF) {
            obfuscatedKey = licenseKey.substring(0, KEY_LENGTH_CUTOFF) +
                    String.join("", Collections.nCopies(keyLength - KEY_LENGTH_CUTOFF, "*"));
        } else {
            obfuscatedKey = String.join("", Collections.nCopies(keyLength, "*"));
        }

        return obfuscatedKey;
    }
}
