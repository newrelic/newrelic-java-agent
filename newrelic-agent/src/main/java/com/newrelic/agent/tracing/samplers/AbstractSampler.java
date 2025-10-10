/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

public interface AbstractSampler {
    /**
     * Available sampler types
     */
    enum SamplerType {
        ADAPTIVE("adaptive"),
        ALWAYS_ON("always_on"),
        ALWAYS_OFF("always_off"),
        PROBABILITY("probability"),
        TRACE_RATIO("trace_ratio");

        private final String description;

        private SamplerType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Calculate the priority of a trace. The value returned must be in the range
     * of 0.0f - 2.0f.
     *
     * @param args A varargs array that contains any arguments that are required to
     * calculate the priority.
     *
     * @return a priority value between 0.0f - 2.0f, inclusive
     */
    float calculatePriority(Object... args);

    /**
     * Get the description/name of this Sampler
     *
     * @return the current Sampler's description
     */
    String getType();
}
