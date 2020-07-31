/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IBMUtils {
    private static final Pattern srNumberPattern = Pattern.compile("\\(SR([0-9]+)[^()]*\\)\\s*$");

    /**
     * See JAVA-1206.
     *
     * Default ibm workaround flag to true for known bad or unparsable ibm versions.
     */
    public static boolean getIbmWorkaroundDefault() {
        try {
            String jvmVendor = System.getProperty("java.vendor");
            if ("IBM Corporation".equals(jvmVendor)) {
                String jvmVersion = System.getProperty("java.specification.version", "");
                int srNum = getIbmSRNumber();

                if ("1.7".equals(jvmVersion) && srNum >= 4) {
                    // IBM Ticket says 7.SR3 is fixed, but no jvm can be found to test against
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Parse the IBM Service Refresh patch version out of the system properties.
     *
     * @return the SR[0-9]* int, or -1 if no version can be found.
     */
    public static int getIbmSRNumber() {
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            String runtimeVersion = System.getProperty("java.runtime.version", "");
            Matcher matcher = srNumberPattern.matcher(runtimeVersion);
            if (matcher.find()) {
                return Integer.valueOf(matcher.group(1));
            }
        }
        return -1;
    }
}
