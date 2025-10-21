/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.trace.TransactionGuidFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraceRatioBasedSamplerTest {
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
        TraceRatioBasedSampler sampler = new TraceRatioBasedSampler("foo");
        assertEquals(0L, sampler.getThreshold());
    }

    @Test
    public void sampler_suppliedInvalidTraceId_returns0Priority() {
        TraceRatioBasedSampler sampler = new TraceRatioBasedSampler(0.1f);
        assertEquals(0.0f, sampler.calculatePriority(null), 0.0f);
    }

    private int runSamplerWith(int iterationCount, float samplingRatio) {
        int iterations = 0;
        int sampledCount = 0;

        Transaction tx = mock(Transaction.class);

        TraceRatioBasedSampler sampler = new TraceRatioBasedSampler(samplingRatio);

        while (++iterations <= iterationCount) {
            when(tx.getOrCreateTraceId()).thenReturn(TransactionGuidFactory.generate16CharGuid() + TransactionGuidFactory.generate16CharGuid());
            if (sampler.calculatePriority(tx) == 2.0f) {
                sampledCount++;
            }
        }

        return sampledCount;
    }
}
