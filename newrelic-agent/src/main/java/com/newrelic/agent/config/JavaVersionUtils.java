/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.regex.Pattern;

/**
 * Simple set of utilities to help us validate that the agent is being run on a supported version of Java.
 */
public class JavaVersionUtils {
    // For Java 21 - 29, the regex should be updated to utilize a range like the 10-19 version check does
    private static final Pattern SUPPORTED_JAVA_VERSION_PATTERN = Pattern.compile("^(1\\.8|9|1[0-9]|2[0-1])$");
    private static final Pattern EXCLUSIVE_MIN_JAVA_VERSION_PATTERN = Pattern.compile("^1\\.7$");
    private static final Pattern EXCLUSIVE_MAX_JAVA_VERSION_PATTERN = Pattern.compile("^22$");
    private static final String MAX_SUPPORTED_VERSION = "21";

    public static final String JAVA_7 = "1.7";
    public static final String JAVA_8 = "1.8";
    public static final String JAVA_9 = "9";
    public static final String JAVA_10 = "10";
    public static final String JAVA_11 = "11";
    public static final String JAVA_12 = "12";
    public static final String JAVA_13 = "13";
    public static final String JAVA_14 = "14";
    public static final String JAVA_15 = "15";
    public static final String JAVA_16 = "16";
    public static final String JAVA_17 = "17";
    public static final String JAVA_18 = "18";
    public static final String JAVA_19 = "19";
    public static final String JAVA_20 = "20";
    public static final String JAVA_21 = "21";
    public static final String JAVA_22 = "22";


    public static String getJavaSpecificationVersion() {
        return System.getProperty("java.specification.version", "");
    }

    public static boolean isAgentSupportedJavaSpecVersion(String javaSpecificationVersion) {
        return javaSpecificationVersion != null && SUPPORTED_JAVA_VERSION_PATTERN.matcher(javaSpecificationVersion).matches();
    }

    /**
     * @param javaSpecificationVersion unsupported java specification version string.
     * @return a printable message for a version of java unsupported by the New Relic agent. Supported versions will return
     * an empty string.
     */
    public static String getUnsupportedAgentJavaSpecVersionMessage(String javaSpecificationVersion) {
        if (javaSpecificationVersion == null) {
            return "";
        }

        StringBuilder message = new StringBuilder();
        if (EXCLUSIVE_MIN_JAVA_VERSION_PATTERN.matcher(javaSpecificationVersion).matches()) {
            message.append("Java version is: ").append(javaSpecificationVersion).append(". ");
            message.append("This version of the New Relic Agent does not support Java 1.7 or below. ")
                    .append("Please use a 6.5.3 New Relic agent or a later version of Java.");
        } else if (EXCLUSIVE_MAX_JAVA_VERSION_PATTERN.matcher(javaSpecificationVersion).matches()) {
            message.append("Java version is: ").append(javaSpecificationVersion).append(". ");
            message.append("This version of the New Relic Agent does not officially support versions of Java greater than ");
            message.append(MAX_SUPPORTED_VERSION);
            message.append(".\n");
            message.append("To enable support for newer versions of Java, the following environment variable or Java system property can be set:\n");
            message.append("\tEnvironment variable: NEW_RELIC_EXPERIMENTAL_RUNTIME=true\n");
            message.append("\tSystem property: newrelic.config.experimental_runtime=true\n");
            message.append("Enabling experimental mode may cause agent issues, application crashes or other problems.");
        }
        return message.toString();
    }
}
