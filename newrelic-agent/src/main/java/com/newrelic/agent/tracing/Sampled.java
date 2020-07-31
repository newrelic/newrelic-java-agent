/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

/**
 * Represents whether or not a New Relic span has been sampled.
 */
public enum Sampled {
    SAMPLED_YES, SAMPLED_NO, UNKNOWN;

    /**
     * Turns a value into a Sampled enum.
     * This isn't a generic boolean parser, but is instead intended to only
     * handle the simple cases offered by DT and W3C headers.
     *
     * @return SAMPLED_YES if "1" or true, SAMPLED_NO if "0" or false, and UNKNOWN otherwise.
     */
    public static Sampled parse(Object value) {
        if ("1".equals(value) || Boolean.TRUE.equals(value)) {
            return SAMPLED_YES;
        }
        if ("0".equals(value) || Boolean.FALSE.equals(value)) {
            return SAMPLED_NO;
        }
        return UNKNOWN;
    }

    public boolean booleanValue() {
        return this == SAMPLED_YES;
    }
}
