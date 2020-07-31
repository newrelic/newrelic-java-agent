/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

public interface CountStats extends StatsBase {
    /**
     * Increment the call count by one.
     */
    void incrementCallCount();

    /**
     * Increment the call count by the given value.
     */
    void incrementCallCount(int value);

    /**
     * Returns the invocation count.
     * 
     */
    int getCallCount();

    /**
     * Sets the invocation count.
     * 
     * @param count
     */
    void setCallCount(int count);

    /**
     * Returns the sum of the data points recorded.
     * 
     */
    float getTotal();

    float getTotalExclusiveTime();

    float getMinCallTime();

    float getMaxCallTime();

    double getSumOfSquares();
}
