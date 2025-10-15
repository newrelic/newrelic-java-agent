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
     * Validate and convert the args[0] value into a float.
     *
     * @param args the argument array to validate
     *
     * @return the float value from args[0] or Float.NaN if the value is invalid
     */
    static float samplingProbabilityFromVarArgs(Object... args) {
        if (args.length == 1 && args[0] instanceof Float) {
            return (float) args[0];
        } else {
            return Float.NaN;
        }
    }

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
}
