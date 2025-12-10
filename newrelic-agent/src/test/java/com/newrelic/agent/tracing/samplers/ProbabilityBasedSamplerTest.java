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
import com.newrelic.agent.tracing.Granularity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ProbabilityBasedSamplerTest {
    SamplerConfig mockSamplerConfig;

    @Before
    public void setup() {
        mockSamplerConfig = mock(SamplerConfig.class);
        when(mockSamplerConfig.getSamplerType()).thenReturn("probability");
    }

    @Test
    public void samplerHonorsConfiguredProbability() {
        int iterations = 10000;
        float probability = 0.1f;
        float maxExpectedErrorRate = 0.01f;
        int sampledCount = runSamplerWith(iterations, probability);

        int expectedToBeSampled = (int)(iterations * probability);
        int errorMargin = (int)(iterations * maxExpectedErrorRate);

        System.out.printf("Sample size: %d; Expected to be sampled: %d; Actual sampled count: %d; " +
                "Allowable error margin: %d\n", iterations, expectedToBeSampled, sampledCount, errorMargin);

        assertTrue(Math.abs(expectedToBeSampled - sampledCount) <= errorMargin);
    }

    @Test
    public void sampler_configuredWith100PercentProbability_samplesEverything() {
        int sampledCount = runSamplerWith(10000, 1.0f);
        assertEquals(10000, sampledCount);
    }

    @Test
    public void sampler_configuredWith0PercentProbability_samplesNothing() {
        int sampledCount = runSamplerWith(10000, 0.0f);
        assertEquals(0, sampledCount);
    }

    @Test
    public void sampler_configuredWith100PercentProbability_hasThresholdValueOf0() {
        when(mockSamplerConfig.getSamplerRatio()).thenReturn(1.0f);
        ProbabilityBasedSampler sampler = new ProbabilityBasedSampler(mockSamplerConfig);
        assertEquals(0, sampler.getRejectionThreshold());
    }

    @Test
    public void sampler_initializedWithInvalidRatioArg_returnsMaxThreshold() {
        when(mockSamplerConfig.getSamplerRatio()).thenReturn(Float.NaN);
        ProbabilityBasedSampler sampler = new ProbabilityBasedSampler(mockSamplerConfig);
        assertEquals((long) Math.pow(2, 56), sampler.getRejectionThreshold());
    }

    @Test
    public void sampler_configuredWith0PercentProbability_hasMaxThresholdValue() {
        when(mockSamplerConfig.getSamplerRatio()).thenReturn(0.0f);
        ProbabilityBasedSampler sampler = new ProbabilityBasedSampler(mockSamplerConfig);
        assertEquals((long) Math.pow(2, 56), sampler.getRejectionThreshold());
    }

    @Test
    public void sampler_suppliedInvalidTraceId_returns0Priority() {
        when(mockSamplerConfig.getSamplerRatio()).thenReturn(0.5f);
        ProbabilityBasedSampler sampler = new ProbabilityBasedSampler(mockSamplerConfig);
        assertEquals(0.0f, sampler.calculatePriority(null, Granularity.FULL), 0.0f);
    }


    private int runSamplerWith(int iterationCount, float samplingProbability) {
        int iterations = 0;
        int sampledCount = 0;

        Transaction tx = mock(Transaction.class);
        when(tx.getOrCreateTraceId()).thenReturn(TransactionGuidFactory.generate16CharGuid() + TransactionGuidFactory.generate16CharGuid());

        when(mockSamplerConfig.getSamplerRatio()).thenReturn(samplingProbability);
        ProbabilityBasedSampler sampler = new ProbabilityBasedSampler(mockSamplerConfig);

        while (++iterations <= iterationCount) {
            when(tx.getOrCreateTraceId()).thenReturn(TransactionGuidFactory.generate16CharGuid() + TransactionGuidFactory.generate16CharGuid());
            if (sampler.calculatePriority(tx, Granularity.FULL) >= 1.0f) {
                sampledCount++;
            }
        }

        return sampledCount;
    }
}
