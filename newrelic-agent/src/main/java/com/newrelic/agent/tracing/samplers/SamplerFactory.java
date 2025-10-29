/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.config.coretracing.SamplerConfig;

/**
 * Factory to create instances of the various samplers used by the agent.
 */
public class SamplerFactory {
    public static final String  ADAPTIVE = "adaptive";
    public static final String  ALWAYS_ON = "always_on";
    public static final String  ALWAYS_OFF = "always_off";
    public static final String  PROBABILITY = "probability";
    public static final String TRACE_RATIO_ID_BASED = "trace_id_ratio_based";
    public static final String  DEFAULT = "default";

    /**
     * Factory method to create an instance of a Sampler
     *
     * @param samplerConfig the agent's finalized sampler configuration
     *
     * @return the constructed Sampler instance
     */
    public static Sampler createSampler(SamplerConfig samplerConfig) {
        switch (samplerConfig.getSamplerType()) {
            case ALWAYS_ON:
                return new AlwaysOnSampler();

            case ALWAYS_OFF:
                return new AlwaysOffSampler();

            case PROBABILITY:
                return new ProbabilityBasedSampler(samplerConfig);

            case TRACE_RATIO_ID_BASED:
                return new TraceRatioBasedSampler(samplerConfig);

            default:
                return AdaptiveSampler.getSharedInstance();
        }
    }
}
