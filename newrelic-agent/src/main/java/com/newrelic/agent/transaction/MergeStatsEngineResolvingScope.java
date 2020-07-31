/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;

public class MergeStatsEngineResolvingScope implements StatsWork {

    private final String appName;
    private final TransactionStats statsEngine;
    private final String resolvedScope;

    public MergeStatsEngineResolvingScope(String resolvedScope, String appName, TransactionStats statsEngine) {
        this.resolvedScope = resolvedScope;
        this.appName = appName;
        this.statsEngine = statsEngine;
    }

    @Override
    public void doWork(StatsEngine statsEngine) {
        statsEngine.mergeStatsResolvingScope(this.statsEngine, resolvedScope);
    }

    @Override
    public String getAppName() {
        return appName;
    }
}
