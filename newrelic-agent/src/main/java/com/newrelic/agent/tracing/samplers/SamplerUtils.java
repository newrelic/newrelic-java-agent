/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

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
     * Validate and convert the args[0] value into a String
     *
     * @param args the argument array to validate
     *
     * @return the String value from args[0] or null if the value is invalid
     */
    static String traceIdFromVarArgs(Object... args) {
        if (args.length == 1 && args[0] instanceof String) {
            return (String) args[0];
        } else {
            return null;
        }
    }
}
