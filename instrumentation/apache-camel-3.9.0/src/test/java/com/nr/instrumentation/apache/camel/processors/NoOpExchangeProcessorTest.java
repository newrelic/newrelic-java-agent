/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.apache.camel.processors;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class NoOpExchangeProcessorTest {

    @Test
    public void testNoOpExchangeProcessorNotStartTxns() {
        assertFalse(new NoOpExchangeProcessor().shouldStartTransaction());
    }
}
