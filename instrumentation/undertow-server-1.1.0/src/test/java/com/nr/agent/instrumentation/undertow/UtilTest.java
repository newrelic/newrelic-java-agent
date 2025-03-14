/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.undertow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest {
    @Test
    public void createTransactionName_returnsCorrectName() {
        assertEquals("my/path (GET)", Util.createTransactionName("my/path", "GET"));
        assertEquals("(Unknown) (Unknown)", Util.createTransactionName(null, null));
        assertEquals("(Unknown) (GET)", Util.createTransactionName(null, "GET"));
        assertEquals("my/path (Unknown)", Util.createTransactionName("my/path", null));
    }
}
