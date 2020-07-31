/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.newrelic.agent.TransactionErrorPriority.API;
import static com.newrelic.agent.TransactionErrorPriority.TRACER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionErrorPriorityTest {
    private static AtomicReference<TransactionErrorPriority> ref(TransactionErrorPriority ref) {
        return new AtomicReference<>(ref);
    }

    @Test
    public final void testNull() {
        assertTrue(API.updateCurrentPriority(ref(null)));
        assertTrue(TRACER.updateCurrentPriority(ref(null)));
    }

    @Test
    public final void testAPI() {
        assertFalse(API.updateCurrentPriority(ref(API)));
        assertFalse(TRACER.updateCurrentPriority(ref(API)));
    }

    @Test
    public final void testTracer() {
        assertTrue(API.updateCurrentPriority(ref(TRACER)));
        assertTrue(TRACER.updateCurrentPriority(ref(TRACER)));
    }
}
