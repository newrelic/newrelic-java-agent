/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.lambda;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LambdaMetadataProviderTest {

    @Before
    public void setUp() {
        // Clear any previous state
        LambdaMetadataProvider.clear();
    }

    @After
    public void tearDown() {
        LambdaMetadataProvider.clear();
    }

    @Test
    public void testSetAndGetArn() {
        String testArn = "arn:aws:lambda:us-east-1:123456789012:function:my-function";
        LambdaMetadataProvider.setArn(testArn);

        String retrievedArn = LambdaMetadataProvider.getArn();
        assertEquals(testArn, retrievedArn);
    }

    @Test
    public void testSetAndGetFunctionVersion() {
        String testVersion = "$LATEST";
        LambdaMetadataProvider.setFunctionVersion(testVersion);

        String retrievedVersion = LambdaMetadataProvider.getFunctionVersion();
        assertEquals(testVersion, retrievedVersion);
    }

    @Test
    public void testClear() {
        LambdaMetadataProvider.setArn("arn:aws:lambda:us-east-1:123456789012:function:test");
        LambdaMetadataProvider.setFunctionVersion("1");

        LambdaMetadataProvider.clear();

        assertNull(LambdaMetadataProvider.getArn());
        assertNull(LambdaMetadataProvider.getFunctionVersion());
    }

    @Test
    public void testNullValues() {
        LambdaMetadataProvider.setArn(null);
        LambdaMetadataProvider.setFunctionVersion(null);

        assertNull(LambdaMetadataProvider.getArn());
        assertNull(LambdaMetadataProvider.getFunctionVersion());
    }

    @Test
    public void testEmptyStrings() {
        LambdaMetadataProvider.setArn("");
        LambdaMetadataProvider.setFunctionVersion("");

        assertEquals("", LambdaMetadataProvider.getArn());
        assertEquals("", LambdaMetadataProvider.getFunctionVersion());
    }

    @Test
    public void testOverwriteValues() {
        LambdaMetadataProvider.setArn("arn:first");
        LambdaMetadataProvider.setFunctionVersion("v1");

        LambdaMetadataProvider.setArn("arn:second");
        LambdaMetadataProvider.setFunctionVersion("v2");

        assertEquals("arn:second", LambdaMetadataProvider.getArn());
        assertEquals("v2", LambdaMetadataProvider.getFunctionVersion());
    }
}
