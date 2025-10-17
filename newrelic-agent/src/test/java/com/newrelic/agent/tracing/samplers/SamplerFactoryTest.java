/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.MockServiceManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SamplerFactoryTest {
    private MockServiceManager serviceManager;

    @Before
    public void setup(){
        serviceManager = new MockServiceManager();
    }

    @Test
    public void testGetSamplerForType(){
        assertEquals("always_on", SamplerFactory.createSampler(SamplerFactory.ALWAYS_ON).getType());
        assertEquals("always_off", SamplerFactory.createSampler(SamplerFactory.ALWAYS_OFF).getType());
        assertEquals("probability", SamplerFactory.createSampler(SamplerFactory.PROBABILITY).getType());
        assertEquals("trace_ratio", SamplerFactory.createSampler(SamplerFactory.TRACE_RATIO).getType());
        assertEquals("adaptive", SamplerFactory.createSampler(SamplerFactory.ADAPTIVE).getType());
        assertEquals("adaptive", SamplerFactory.createSampler(SamplerFactory.DEFAULT).getType());
        assertEquals("adaptive", SamplerFactory.createSampler("").getType());
    }

}