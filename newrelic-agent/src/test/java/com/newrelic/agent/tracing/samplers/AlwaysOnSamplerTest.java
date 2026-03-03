/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracing.Granularity;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class AlwaysOnSamplerTest {

    @Test
    public void testAlwaysOnSampler(){
        AlwaysOnSampler sampler = new AlwaysOnSampler();

        Transaction tx = Mockito.mock(Transaction.class);
        assertEquals(3.0f, sampler.calculatePriority(tx, Granularity.FULL), 0.0f);
        assertEquals(2.0f, sampler.calculatePriority(tx, Granularity.PARTIAL), 0.0f);
    }

}
