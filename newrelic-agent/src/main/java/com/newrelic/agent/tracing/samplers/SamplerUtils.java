/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.Transaction;

public class SamplerUtils {
    /**
     * Extract the trace id from the supplied Transaction instance
     *
     * @param tx The target Transaction
     *
     * @return the extracted trace id or null if the Transaction instance is null
     */
    static String traceIdFromTransaction(Transaction tx) {
        if (tx != null) {
            return tx.getOrCreateTraceId();
        } else {
            return null;
        }
    }

    /**
     * Determine if the supplied float value is a valid value for the ratio value
     *
     * @param ratio the ration value to check
     *
     * @return true if the ratio is valid; false otherwise
     */
    static boolean isValidTraceRatio(float ratio) {
        return ratio >= 0.0f && ratio <= 1.0f;
    }
}
