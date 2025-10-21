/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.SamplerConfig;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import static org.junit.Assert.assertEquals;

public class SamplerFactoryTest {
    private MockServiceManager serviceManager;

    @Before
    public void setup(){
        serviceManager = new MockServiceManager();
    }

    @Test
    public void testGetSamplerForType() {
        SamplerConfig mockSamplerConfig = mock(SamplerConfig.class);
        when(mockSamplerConfig.getSamplerRatio()).thenReturn(.5f);

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerFactory.ALWAYS_ON);
        assertEquals("always_on", SamplerFactory.createSampler(mockSamplerConfig));

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerFactory.ALWAYS_OFF);
        assertEquals("always_off", SamplerFactory.createSampler(mockSamplerConfig));

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerFactory.PROBABILITY);
        assertEquals("probability", SamplerFactory.createSampler(mockSamplerConfig));

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerFactory.TRACE_RATIO);
        assertEquals("trace_ratio", SamplerFactory.createSampler(mockSamplerConfig));

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerFactory.ADAPTIVE);
        assertEquals("adaptive", SamplerFactory.createSampler(mockSamplerConfig));

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerFactory.DEFAULT);
        assertEquals("adaptive", SamplerFactory.createSampler(mockSamplerConfig));

        when(mockSamplerConfig.getSamplerType()).thenReturn("foo");
        assertEquals("adaptive", SamplerFactory.createSampler(mockSamplerConfig));
    }
}