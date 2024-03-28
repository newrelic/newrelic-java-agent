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

    @Test
    public void supportAgentJavaSpecVersions() {
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_8));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_9));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_10));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_11));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_12));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_13));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_14));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_15));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_16));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_17));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_18));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_19));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_20));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_21));
        assertTrue(JavaVersionUtils.isAgentSupportedJavaSpecVersion(JavaVersionUtils.JAVA_22));
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
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23."));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23.0"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23+181"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23.0+181"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23.0_b181"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23.0.1"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23.0.1+11"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23.0.1_11"));
        assertFalse(JavaVersionUtils.isAgentSupportedJavaSpecVersion("23.0.1_11-b11"));
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
        String msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JavaVersionUtils.JAVA_7);
        assertThat(msg, containsString(JavaVersionUtils.JAVA_7));
        assertThat(msg, containsString("6.5.3 New Relic agent"));
    }

    @Test
    public void unsupportedJavaVersionMessageWhenGreaterThanMaxSupportedVersion() {
        String msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JavaVersionUtils.JAVA_23);
        assertThat(msg, containsString(JavaVersionUtils.JAVA_23));
        assertThat(msg, containsString("Java greater than 22."));
    }

    @Test
    public void emptyMessageReturnedWhenJavaVersionSupported() {
        String msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JavaVersionUtils.JAVA_8);
        assertEquals(0, msg.length());

        msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(JavaVersionUtils.JAVA_22);
        assertEquals(0, msg.length());

        msg = JavaVersionUtils.getUnsupportedAgentJavaSpecVersionMessage(null);
        assertEquals(0, msg.length());
    }

}
