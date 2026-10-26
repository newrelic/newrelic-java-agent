/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.virtualthreads;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualThreadUtilsTest {

    @Test
    public void isSupportedJavaVersion_supportedCutoffVersion() {
        assertTrue(VirtualThreadUtils.isSupportedJavaVersion("24"));
    }

    @Test
    public void isSupportedJavaVersion_aboveSupportedVersion() {
        assertTrue(VirtualThreadUtils.isSupportedJavaVersion("25"));
    }

    @Test
    public void isSupportedJavaVersion_belowSupportedVersion() {
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion("23"));
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion("17"));
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion("11"));
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion("9"));
    }

    @Test
    public void isSupportedJavaVersion_legacyJava8Format() {
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion("1.8"));
    }

    @Test
    public void isSupportedJavaVersion_null() {
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion(null));
    }

    @Test
    public void isSupportedJavaVersion_empty() {
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion(""));
    }

    @Test
    public void isSupportedJavaVersion_unparseableGarbage() {
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion("not-a-version"));
    }

    @Test
    public void isSupportedJavaVersion_unexpectedDottedFormat() {
        // The java.specification.version property should never report a version
        // in this format, but we catch the NFE and return false just in case
        assertFalse(VirtualThreadUtils.isSupportedJavaVersion("24.0.1"));
    }
}