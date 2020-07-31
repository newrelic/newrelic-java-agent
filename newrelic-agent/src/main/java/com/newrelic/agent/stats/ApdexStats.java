/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

public interface ApdexStats extends StatsBase {

    void recordApdexFrustrated();

    /**
     * Apdex metrics are special case values where we overload some fields inside the stats to store transaction counts
     * by buckets
     */
    void recordApdexResponseTime(long responseTimeMillis, long apdexIInMillis);

    /**
     * For testing
     */
    int getApdexSatisfying();

    /**
     * For testing
     */
    int getApdexTolerating();

    /**
     * For testing
     */
    int getApdexFrustrating();

}
