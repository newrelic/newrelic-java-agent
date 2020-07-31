/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

/**
 * Holds the scoped and unscoped stats for a single transaction.
 */
public class TransactionStats {

    private final SimpleStatsEngine unscopedStats = new SimpleStatsEngine(16);
    private final SimpleStatsEngine scopedStats = new SimpleStatsEngine();

    public SimpleStatsEngine getUnscopedStats() {
        return unscopedStats;
    }

    /**
     * Returns the scoped stats. Only use this if you know what you're doing. Tracers already use this. If you call this
     * method from the old instrumentation, you'll very likely double account for time. Ask Roger or Saxon if you have
     * questions about this.
     * 
     */
    public SimpleStatsEngine getScopedStats() {
        return scopedStats;
    }

    public int getSize() {
        return unscopedStats.getStatsMap().size() + scopedStats.getStatsMap().size();
    }

    @Override
    public String toString() {
        return "TransactionStats [unscopedStats=" + unscopedStats + ", scopedStats=" + scopedStats + "]";
    }

}
