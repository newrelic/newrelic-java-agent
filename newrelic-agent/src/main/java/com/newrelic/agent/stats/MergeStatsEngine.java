/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

public class MergeStatsEngine implements StatsWork {

    private final String appName;
    private final StatsEngine statsEngine;

    public MergeStatsEngine(String appName, StatsEngine statsEngine) {
        this.appName = appName;
        this.statsEngine = statsEngine;
    }

    @Override
    public void doWork(StatsEngine statsEngine) {
        statsEngine.mergeStats(this.statsEngine);
    }

    @Override
    public String getAppName() {
        return appName;
    }
}
