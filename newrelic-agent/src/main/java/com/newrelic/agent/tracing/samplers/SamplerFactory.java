/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

/**
 * Factory to create instances of the various samplers used by the agent.
 */
public class SamplerFactory {

    /**
     * Factory method to create an instance of a Sampler
     *
     * @param type The SamplerType to create
     * @param args a varargs array that contains parameters used to initialize the target sampler
     *
     * @return the constructed Sampler instance
     */
    public static AbstractSampler createSampler(AbstractSampler.SamplerType type, Object... args) {
        switch (type) {
            case ALWAYS_ON:
                break;

            case ALWAYS_OFF:
                break;

            case PROBABILITY:
                return new ProbabilityBasedSampler(args);

            case TRACE_RATIO:
                return new TraceRatioBasedSampler(args);

            default:
                break;
        }

        return null;
    }
}
