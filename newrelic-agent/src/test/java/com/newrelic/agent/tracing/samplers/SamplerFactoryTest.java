/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.coretracing.SamplerConfig;
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

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerConfig.ALWAYS_ON);
        assertEquals(SamplerType.ALWAYS_ON, SamplerFactory.createSampler(mockSamplerConfig).getType());

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerConfig.ALWAYS_OFF);
        assertEquals(SamplerType.ALWAYS_OFF, SamplerFactory.createSampler(mockSamplerConfig).getType());

        when(mockSamplerConfig.getSamplerType()).thenReturn("probability"); //this one is not supported in config yet
        assertEquals(SamplerType.PROBABILITY, SamplerFactory.createSampler(mockSamplerConfig).getType());

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerConfig.TRACE_ID_RATIO_BASED);
        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, SamplerFactory.createSampler(mockSamplerConfig).getType());

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerConfig.ADAPTIVE);
        assertEquals(SamplerType.ADAPTIVE, SamplerFactory.createSampler(mockSamplerConfig).getType());

        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerConfig.DEFAULT);
        assertEquals(SamplerType.ADAPTIVE, SamplerFactory.createSampler(mockSamplerConfig).getType());

        when(mockSamplerConfig.getSamplerType()).thenReturn("foo");
        assertEquals(SamplerType.ADAPTIVE, SamplerFactory.createSampler(mockSamplerConfig).getType());
    }
}