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
    public static final String  ADAPTIVE = "adaptive";
    public static final String  ALWAYS_ON = "always_on";
    public static final String  ALWAYS_OFF = "always_off";
    public static final String  PROBABILITY = "probability";
    public static final String  TRACE_RATIO = "trace_ratio";
    public static final String  DEFAULT = "default";

    /**
     * Factory method to create an instance of a Sampler
     *
     * @param type the SamplerType to create as a String
     * @param args a varargs array that contains parameters used to initialize the target sampler
     *
     * @return the constructed Sampler instance
     */
    public static Sampler createSampler(String type, Object... args) {
        switch (type) {
            case "always_on":
                return new AlwaysOnSampler();

            case "always_off":
                return new AlwaysOffSampler();

            case "probability":
                return new ProbabilityBasedSampler(args);

            case "trace_ratio":
                return new TraceRatioBasedSampler(args);

            default:
                return AdaptiveSampler.getSharedInstance();
        }
    }
}
