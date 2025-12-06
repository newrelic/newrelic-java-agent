/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.agent.trace.TransactionGuidFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraceRatioBasedSamplerTest {
    SamplerConfig mockSamplerConfig;

    @Before
    public void setup() {
        mockSamplerConfig = mock(SamplerConfig.class);
        when(mockSamplerConfig.getSamplerType()).thenReturn(SamplerConfig.TRACE_ID_RATIO_BASED);
    }

    @Test
    public void samplerHonorsConfiguredRatio() {
        int iterations = 10000;
        float ratio = 0.1f;
        float maxExpectedErrorRate = 0.01f;
        int sampledCount = runSamplerWith(iterations, ratio);

        int expectedToBeSampled = (int)(iterations * ratio);
        int errorMargin = (int)(iterations * maxExpectedErrorRate);

        System.out.printf("Sample size: %d; Expected to be sampled: %d; Actual sampled count: %d; " +
                "Allowable error margin: %d\n", iterations, expectedToBeSampled, sampledCount, errorMargin);

        assertTrue(expectedToBeSampled + errorMargin >= sampledCount);
        assertTrue(Math.abs(expectedToBeSampled - sampledCount) <= errorMargin);
    }

    @Test
    public void sampler_configuredWith100PercentRatio_samplesEverything() {
        int sampledCount = runSamplerWith(10000, 1.0f);
        assertEquals(10000, sampledCount);
    }

    @Test
    public void sampler_configuredWith0PercentRatio_samplesNothing() {
        int sampledCount = runSamplerWith(10000, 0.0f);
        assertEquals(0, sampledCount);
    }
    @Test
    public void sampler_initializedWithInvalidRatioArg_returns0Threshold() {
        when(mockSamplerConfig.getSamplerRatio()).thenReturn(Float.NaN);
        TraceRatioBasedSampler sampler = new TraceRatioBasedSampler(mockSamplerConfig);
        assertEquals(0L, sampler.getThreshold());
    }

    @Test
    public void sampler_suppliedInvalidTraceId_returns0Priority() {
        when(mockSamplerConfig.getSamplerRatio()).thenReturn(0.5f);
        TraceRatioBasedSampler sampler = new TraceRatioBasedSampler(mockSamplerConfig);
        assertEquals(0.0f, sampler.calculatePriority(null), 0.0f);
    }

    @Test
    public void isValidTraceRatio_returnsCorrectValue() {
        assertTrue(Sampler.isValidTraceRatio(0.0f));
        assertTrue(Sampler.isValidTraceRatio(0.5f));
        assertTrue(Sampler.isValidTraceRatio(1.0f));

        assertFalse(Sampler.isValidTraceRatio(-0.1f));
        assertFalse(Sampler.isValidTraceRatio(1.1f));
    }

    @Test
    public void traceIdFromTransaction_withValildTxn_returnsTraceId() {
        Transaction tx = mock(Transaction.class);
        when(tx.getOrCreateTraceId()).thenReturn("123");
        assertEquals("123", Sampler.traceIdFromTransaction(tx));
    }

    @Test
    public void traceIdFromTransaction_withNullTxn_returnsNull() {
        assertNull(Sampler.traceIdFromTransaction(null));
    }

    private int runSamplerWith(int iterationCount, float samplingRatio) {
        int iterations = 0;
        int sampledCount = 0;

        Transaction tx = mock(Transaction.class);

        when(mockSamplerConfig.getSamplerRatio()).thenReturn(samplingRatio);
        TraceRatioBasedSampler sampler = new TraceRatioBasedSampler(mockSamplerConfig);

        while (++iterations <= iterationCount) {
            when(tx.getOrCreateTraceId()).thenReturn(TransactionGuidFactory.generate16CharGuid() + TransactionGuidFactory.generate16CharGuid());
            if (sampler.calculatePriority(tx) >= 1.0f) {
                sampledCount++;
            }
        }

        return sampledCount;
    }
}
