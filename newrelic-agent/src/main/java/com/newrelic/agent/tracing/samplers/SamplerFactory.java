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

    /**
     * Factory method to create an instance of a Sampler
     *
     * @param samplerConfig the agent's finalized sampler configuration
     *
     * @return the constructed Sampler instance
     */
    public static Sampler createSampler(SamplerConfig samplerConfig) {
        final String PROBABILITY = "probability"; //This setting is not yet enabled in config. Wired here for future use.

        switch (samplerConfig.getSamplerType()) {
            case SamplerConfig.ALWAYS_ON:
                return new AlwaysOnSampler();

            case SamplerConfig.ALWAYS_OFF:
                return new AlwaysOffSampler();

            case SamplerConfig.TRACE_ID_RATIO_BASED:
                return new TraceRatioBasedSampler(samplerConfig);

            case PROBABILITY:
                return new ProbabilityBasedSampler(samplerConfig);

            default:
                return AdaptiveSampler.getAdaptiveSampler(samplerConfig);
        }
    }
}
