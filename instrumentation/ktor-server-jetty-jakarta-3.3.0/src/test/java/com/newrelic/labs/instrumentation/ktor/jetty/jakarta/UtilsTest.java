/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.jetty.jakarta;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void getEnhancedTransactionName_withSimplePath() {
        assertEquals("api/users - {GET}", Utils.getEnhancedTransactionName("/api/users", "GET"));
    }

    @Test
    public void getEnhancedTransactionName_withRootUri() {
        assertEquals("Root - {POST}", Utils.getEnhancedTransactionName("/", "POST"));
    }

    @Test
    public void getEnhancedTransactionName_withQueryString() {
        assertEquals("search - {GET}", Utils.getEnhancedTransactionName("/search?q=test", "GET"));
    }

    @Test
    public void getEnhancedTransactionName_withDeepPath() {
        assertEquals("a/b/c/d - {GET}", Utils.getEnhancedTransactionName("/a/b/c/d", "GET"));
    }

    @Test
    public void getEnhancedTransactionName_withNullUri() {
        assertEquals("Unknown - {GET}", Utils.getEnhancedTransactionName(null, "GET"));
    }

    @Test
    public void getEnhancedTransactionName_withNullMethod() {
        assertEquals("path", Utils.getEnhancedTransactionName("/path", null));
    }
}
