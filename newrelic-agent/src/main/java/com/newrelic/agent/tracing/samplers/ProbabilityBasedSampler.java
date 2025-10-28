/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.SamplerConfig;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

/**
 * Probability based sampler that utilizes the hex encoded
 * trace id string as a deterministic source of randomness.
 * <br>
 * The sampler is seeded with a float value that represents the sampling
 * probability target. The threshold (T) is calculated via:
 * <pre>
 *     (1 - samplingProbability) * 2^56
 * </pre>
 * When the sampler is presented with the hex encoded trace id, a
 * deterministic random value (R) is derived by extracting the last 7 bytes
 * (14 characters) of the id and converting into a long value.
 * <br>
 * If this value is greater than or equal to T, we return a priority of 2.0 which
 * will mark this trace for sampling.
 */
public class ProbabilityBasedSampler implements Sampler {
    private final long rejectionThreshold;

    /**
     * Construct a new ProbabilityBasedSampler with the desired probability
     * supplied as a float value in the SamplerConfig instance.
     *
     * @param samplerConfig the agent's finalized sampler configuration
     */
    public ProbabilityBasedSampler(SamplerConfig samplerConfig) {
        float samplingProbability = samplerConfig.getSamplerRatio();
        if (!Float.isNaN(samplingProbability)) {
            this.rejectionThreshold = (long) ((1 - samplingProbability) * Math.pow(2, 56));
            NewRelic.getAgent().getLogger().log(Level.INFO, "ProbabilityBasedSampler: rejection threshold {0}", rejectionThreshold);
        } else {
            this.rejectionThreshold = (long) Math.pow(2, 56);
            NewRelic.getAgent().getLogger().log(Level.WARNING, "ProbabilityBasedSampler: Invalid sampling probability supplied; setting " +
                            "rejection threshold to {0}", rejectionThreshold);
        }
    }

    @Override
    public float calculatePriority(Transaction tx) {
        String traceId = Sampler.traceIdFromTransaction(tx);
        if (traceId != null && traceId.length() == 32) {
            try {
                String last14Chars = traceId.substring(18);
                return Long.parseUnsignedLong(last14Chars, 16) >= rejectionThreshold ? 2.0f : 0.0f;
            } catch (NumberFormatException ignored) {
            }
        }

        return 0.0f;
    }

    @Override
    public String getType() {
        return SamplerFactory.PROBABILITY;
    }

    /**
     * Retrieve the current rejection threshold value
     *
     * @return the calculated rejection threshold
     */
    public long getRejectionThreshold() {
        return rejectionThreshold;
    }
}
