/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.service.EventService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventsServiceImpl;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.StatsWorks;

import java.util.concurrent.TimeUnit;

public abstract class Harvestable {

    private final EventService service;
    private final String appName;
    private volatile long lastHarvest;

    public Harvestable(EventService service, String appName) {
        this.service = service;
        this.appName = appName;
        this.lastHarvest = System.nanoTime();
    }

    public abstract String getEndpointMethodName();

    public abstract int getMaxSamplesStored();

    public void harvest() {
        recordIntervalMetric();
        service.harvestEvents(appName);
    }

    private void recordIntervalMetric() {
        long startTimeInNanos = System.nanoTime();
        final long harvestIntervalInNanos = startTimeInNanos - lastHarvest;
        lastHarvest = startTimeInNanos;

        ServiceFactory.getStatsService().doStatsWork(new StatsWork() {
            @Override
            public void doWork(StatsEngine statsEngine) {
                if (harvestIntervalInNanos > 0) {
                    statsEngine.getResponseTimeStats(
                            service.getEventHarvestIntervalMetric()).recordResponseTime(
                            harvestIntervalInNanos, TimeUnit.NANOSECONDS);
                }
            }

            @Override
            public String getAppName() {
                return appName;
            }
        }, "HarvestInterval");
    }

    public String getAppName() {
        return appName;
    }

    public void configure(long reportPeriodInMillis, int maxSamplesStored) {
        long reportPeriodInSeconds = TimeUnit.MILLISECONDS.toSeconds(reportPeriodInMillis);
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordMetricWork(service.getReportPeriodInSecondsMetric(), reportPeriodInSeconds), service.getReportPeriodInSecondsMetric());
        ServiceFactory.getStatsService().doStatsWork(
                StatsWorks.getRecordMetricWork(service.getEventHarvestLimitMetric(), maxSamplesStored), service.getEventHarvestLimitMetric());

        if (maxSamplesStored != service.getMaxSamplesStored()) {
            service.setMaxSamplesStored(maxSamplesStored);
            service.setReportPeriodInMillis(reportPeriodInMillis);
            maybeSendSpanLimitMetric(maxSamplesStored);
            service.harvestEvents(appName);
            service.clearReservoir();
        }
    }

    private void maybeSendSpanLimitMetric(int maxSamplesStored) {
        if (service instanceof SpanEventsServiceImpl) {
            ServiceFactory.getStatsService().doStatsWork(
                    StatsWorks.getRecordMetricWork(MetricNames.SUPPORTABILITY_SPAN_EVENT_LIMIT, maxSamplesStored), MetricNames.SUPPORTABILITY_SPAN_EVENT_LIMIT);
        }
    }
}
