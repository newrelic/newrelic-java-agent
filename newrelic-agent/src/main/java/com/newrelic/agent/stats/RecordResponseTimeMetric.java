/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

public final class RecordResponseTimeMetric implements StatsWork {
    private final long totalInMillis;
    private final long exclusiveTimeInMillis;
    private final String name;
    private final TimeUnit timeUnit;

    public RecordResponseTimeMetric(long millis, String name, TimeUnit timeUnit) {
        this(millis, millis, name, timeUnit);
    }

    public RecordResponseTimeMetric(long totalInMillis, long exclusiveTimeInMillis, String name, TimeUnit timeUnit) {
        this.exclusiveTimeInMillis = exclusiveTimeInMillis;
        this.totalInMillis = totalInMillis;
        this.timeUnit = timeUnit;

        this.name = name;
    }

    @Override
    public void doWork(StatsEngine statsEngine) {
        statsEngine.getResponseTimeStats(name).recordResponseTime(totalInMillis, exclusiveTimeInMillis, timeUnit);
    }

    @Override
    public String getAppName() {
        return null;
    }

}
