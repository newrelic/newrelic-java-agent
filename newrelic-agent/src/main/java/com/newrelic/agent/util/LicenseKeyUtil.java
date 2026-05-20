/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.Agent;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseKeyUtil {
    // Matches license_key= (URL) or "license_key":" (JSON) and captures the prefix (group 1) and key value (group 2) separately.
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

        // Avoid regex overhead if string doesn't contain "license_key"
        if (!originalString.contains("license_key")) {
            return originalString;
        }

        Matcher matcher = LICENSE_KEY_PATTERN.matcher(originalString);
        if (!matcher.find()) {
            return originalString;
        }

        // appendReplacement: copies text between matches into sb, then appends the replacement string.
        // quoteReplacement: escapes $ and \ in the replacement so they're treated as literals, not regex tokens.
        // group(1): returns the prefix captured by group 1 (everything up to and including the delimiter).
        // group(2): returns the license key value found in the string, used to drive the obfuscation.
        // find(): advances the matcher to the next occurrence of the pattern.
        // appendTail: flushes any remaining text after the last match into sb.
        StringBuffer sb = new StringBuffer();
        do {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + partialObfuscation(matcher.group(2))));
        } while (matcher.find());
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String partialObfuscation(String licenseKey) {
        int keyLength = licenseKey.length();

        // Per the spec: If length is <= 10, replace the full key with "*"
        // Otherwise, keep the first 10 characters of the key and replace the
        // remaining key characters with "*"
        if (keyLength > KEY_LENGTH_CUTOFF) {
            return licenseKey.substring(0, KEY_LENGTH_CUTOFF) + createAsterisks(keyLength - KEY_LENGTH_CUTOFF);
        } else {
            return createAsterisks(keyLength);
        }
    }

    private static String createAsterisks(int count) {
        char[] asterisks = new char[count];
        Arrays.fill(asterisks, '*');
        return new String(asterisks);
    }
}