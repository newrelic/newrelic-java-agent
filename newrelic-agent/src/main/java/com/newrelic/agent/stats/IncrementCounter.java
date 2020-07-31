/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;


public final class IncrementCounter implements StatsWork {
    private final String name;
    private final int count;

    public IncrementCounter(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    @Override
    public void doWork(StatsEngine statsEngine) {
        statsEngine.getStats(name).incrementCallCount(count);
    }

    @Override
    public String getAppName() {
        return null;
    }

}
