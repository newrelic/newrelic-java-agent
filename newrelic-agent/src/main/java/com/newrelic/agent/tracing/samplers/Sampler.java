/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;

public interface Sampler {
    /**
     * Extract the trace id from the supplied Transaction instance
     *
     * @param tx The target Transaction
     *
     * @return the extracted trace id or null if the Transaction instance is null
     */
    static String traceIdFromTransaction(Transaction tx) {
        return (tx != null ? tx.getOrCreateTraceId() : null);
    }

    /**
     * Determine if the supplied float value is a valid value for the ratio value
     *
     * @param ratio the ratio value to check
     *
     * @return true if the ratio is valid; false otherwise
     */
    static boolean isValidTraceRatio(float ratio) {
        return ratio >= 0.0f && ratio <= 1.0f;
    }

    /**
     * Determine if the supplied int value is a valid value for the adaptive sampling target
     *
     * @param target the target value to check
     *
     * @return true if the target is valid; false otherwise
     */
    static boolean isValidSamplingTarget(int target) {
        return target >= 0 && target <= 120;
    }

    /**
     * Calculate the priority of a trace. The value returned must be in the range
     * of 0.0f - 2.0f.
     *
     * @param tx the transaction to calculate the priority for
     *
     * @return a priority value between 0.0f - 2.0f, inclusive
     */
    float calculatePriority(Transaction tx, Granularity granularity);

    /**
     * Get the type of this Sampler
     *
     * @return the current Sampler's SamplerType
     */
    SamplerType getType();

    /**
     * Human-readable description of this sampler's name and important qualities.
     */
    String getDescription();
}
