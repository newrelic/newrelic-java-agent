/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SampledTest {

    @Test
    public void testParse() throws Exception {
        assertSame(Sampled.SAMPLED_YES, Sampled.parse("1"));
        assertSame(Sampled.SAMPLED_NO, Sampled.parse("0"));
        assertSame(Sampled.SAMPLED_YES, Sampled.parse(true));
        assertSame(Sampled.SAMPLED_NO, Sampled.parse(false));
        assertSame(Sampled.UNKNOWN, Sampled.parse(null));
        assertSame(Sampled.UNKNOWN, Sampled.parse("random"));
        assertSame(Sampled.UNKNOWN, Sampled.parse("yes"));
        assertSame(Sampled.UNKNOWN, Sampled.parse("no"));
    }

    @Test
    public void testBooleanValue() throws Exception {
        assertTrue(Sampled.SAMPLED_YES.booleanValue());
        assertFalse(Sampled.SAMPLED_NO.booleanValue());
        assertFalse(Sampled.UNKNOWN.booleanValue());
    }
}