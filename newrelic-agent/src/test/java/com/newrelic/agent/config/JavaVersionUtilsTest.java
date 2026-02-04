/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JavaVersionUtilsTest {

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
    public static final String JAVA_23 = "23";
    public static final String JAVA_24 = "24";
    public static final String JAVA_25 = "25";
    public static final String JAVA_26 = "26";
    public static final String JAVA_27 = "27";

    @Test
    public void supportAgentJavaSpecVersions() {
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_8));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_9));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_10));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_11));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_12));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_13));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_14));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_15));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_16));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_17));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_18));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_19));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_20));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_21));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_22));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_23));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_24));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_25));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JAVA_26));
    }

    @Test
    public void unsupportedAgentVersionsLessThanJava8() {
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.5"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.5.0"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.5.0_11"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.5.0_11-b12"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.5.1_gibberish"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.6"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.6.0"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.6.0_11"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.6.0_11-b12"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.6.1_gibberish"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.7"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.7.0"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.7.0_11"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.7.0_11-b12"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.7.1_gibberish"));
    }


    @Test
    public void javaVersionHigherThanSupported() {
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27."));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27.0"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27+181"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27.0+181"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27.0_b181"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27.0.1"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27.0.1+11"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27.0.1_11"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("27.0.1_11-b11"));
    }

    @Test
    public void otherUnsupportedVersionStrings() {
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.4"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.4.0"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.4.0_1"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.4.0_1-b01"));

        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("This is not a version string"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("9832423423"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("1.2524234234"));

        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion(""));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion(null));
    }

    @Test
    public void unsupportedJavaVersionMessageWhenLessThanMinimalSupportedVersion() {
        String msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JAVA_7);
        assertThat(msg, containsString(JAVA_7));
        assertThat(msg, containsString("6.5.3 New Relic agent"));
    }

    @Test
    public void unsupportedJavaVersionMessageWhenGreaterThanMaxSupportedVersion() {
        String msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JAVA_27);
        assertThat(msg, containsString(JAVA_27));
        assertThat(msg, containsString("Java greater than 26."));
    }

    @Test
    public void emptyMessageReturnedWhenJavaVersionSupported() {
        String msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JAVA_8);
        assertEquals(0, msg.length());

        msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JAVA_26);
        assertEquals(0, msg.length());

        msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(null);
        assertEquals(0, msg.length());
    }

}
