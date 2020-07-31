/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import java.util.concurrent.TimeUnit;

import com.newrelic.agent.stats.AbstractMetricAggregator;
import com.newrelic.agent.stats.RecordResponseTimeMetric;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;

public class StatsServiceMetricAggregator extends AbstractMetricAggregator {
    private final StatsService statsService;

    public StatsServiceMetricAggregator(StatsService statsService) {
        super();
        this.statsService = statsService;
    }

    @Override
    protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        statsService.doStatsWork(new RecordResponseTimeMetric(totalTime, exclusiveTime, name, timeUnit));
    }

    @Override
    protected void doRecordMetric(String name, float value) {
        statsService.doStatsWork(StatsWorks.getRecordMetricWork(name, value));
    }

    @Override
    protected void doIncrementCounter(String name, int count) {
        statsService.doStatsWork(StatsWorks.getIncrementCounterWork(name, count));
    }

}
