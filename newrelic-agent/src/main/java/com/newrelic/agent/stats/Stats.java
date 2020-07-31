/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

public interface Stats extends CountStats {

    /**
     * Record a single data point with this stats object, which will automatically increment the data point count and
     * track min/max/standard deviation as expected.
     * 
     * @param value
     */
    void recordDataPoint(float value);
}