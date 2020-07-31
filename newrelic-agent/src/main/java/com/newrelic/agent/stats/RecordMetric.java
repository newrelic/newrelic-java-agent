/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

final class RecordMetric implements StatsWork {
    private final String name;
    private final float value;

    public RecordMetric(String name, float value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void doWork(StatsEngine statsEngine) {
        statsEngine.getStats(name).recordDataPoint(value);
    }

    @Override
    public String getAppName() {
        return null;
    }

}
