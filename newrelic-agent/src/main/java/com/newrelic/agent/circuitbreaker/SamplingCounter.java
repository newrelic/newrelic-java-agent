/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.circuitbreaker;

/**
 * Not thread safe. Intended to be used as a ThreadLocal.
 */
public class SamplingCounter {
    private final long samplingRate;
    private long count;

    /**
     * 
     * @param samplingRate sampling rate. Must be greater than zero.
     */
    public SamplingCounter(long samplingRate) {
        count = 0L;
        this.samplingRate = samplingRate;
    }

    /**
     * 
     * Increments count and checks if count is equal to sampling rate.
     * 
     * @return true if count is equal to sampling rate. false otherwise.
     */
    public boolean shouldSample() {
        if (++count > samplingRate) {
            count = 0L;
            return true;
        }
        return false;
    }
}
