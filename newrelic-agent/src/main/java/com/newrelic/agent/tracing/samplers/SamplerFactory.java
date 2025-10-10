/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

public class SamplerFactory {
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
