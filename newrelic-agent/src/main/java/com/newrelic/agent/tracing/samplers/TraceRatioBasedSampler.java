/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

/**
 * Ratio based sampler that utilizes the hex encoded
 * trace id string as a deterministic source of randomness.
 * <br>
 * The sampler is seeded with a float value that represents the sampling
 * ratio target. The threshold (T) is calculated via:
 * <pre>
 *     (Long.MAX_VALUE * traceRatio)
 * </pre>
 * When the sampler is presented with the hex encoded trace id, a
 * deterministic random value (R) is derived by extracting the last 8 bytes
 * (16 characters) of the id and converting into a long value.
 * <br>
 * If this value is less than or equal to T, we return a priority of 2.0 which
 * will mark this trace for sampling.
 */
public class TraceRatioBasedSampler implements Sampler {
    private final long threshold;
    private final float ratio;
    private final String description;

    /**
     * Construct a new TraceRatioBasedSampler with the desired ratio
     * supplied as a float value in the SamplerConfig instance.
     *
     * @param samplerConfig the agent's finalized sampler configuration
     */
    public TraceRatioBasedSampler(SamplerConfig samplerConfig) {
        float traceRatio = samplerConfig.getSamplerRatio();
        if (!Float.isNaN(traceRatio)) {
            threshold = (long) (Long.MAX_VALUE * traceRatio);
            NewRelic.getAgent().getLogger().log(Level.INFO, "TraceRatioBasedSampler: threshold {0}", threshold);
        } else {
            traceRatio = 0.0f;
            threshold = 0L;
            NewRelic.getAgent().getLogger().log(Level.WARNING, "TraceRatioBasedSampler: Invalid sampling ratio supplied; setting " +
                    "threshold to {0}", threshold);
        }
        this.ratio = traceRatio;
        this.description = String.format("Trace Id Ratio Based Sampler, ratio=%.4f", traceRatio);
    }

    @Override
    public float calculatePriority(Transaction tx) {
        String traceId = Sampler.traceIdFromTransaction(tx);

        if (traceId != null && traceId.length() == 32) {
            try {
                String last16Chars = traceId.substring(16);
                return (Math.abs(Long.parseUnsignedLong(last16Chars, 16)) <= threshold) ? 2.0f : 0.0f;
            } catch (NumberFormatException ignored) {
            }
        }

        return 0.0f;
    }

    @Override
    public String getType() {
        return SamplerFactory.TRACE_RATIO_ID_BASED;
    }

    @Override
    public String getDescription(){
        return description;
    }

    /**
     * Retrieve the current rejection threshold value
     *
     * @return the calculated rejection threshold
     */
    public long getThreshold() {
        return threshold;
    }

    public float getRatio() {
        return ratio;
    }
}
