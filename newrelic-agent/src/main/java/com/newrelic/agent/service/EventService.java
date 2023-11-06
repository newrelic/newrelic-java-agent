/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.MetricNames;

/**
 * Defines a service that sends New Relic events. These services can be
 * configured to harvest on a faster interval than other services.
 */
public interface EventService extends Service {

    /**
     * Perform the harvest of any pending events right now.
     *
     * @param appName the application to harvest for
     */
    void harvestEvents(final String appName);

    /**
     * Returns the metric name for this service that records the actual interval in which the harvest is happening. This is different
     * from the report period below because it will be the actual time between harvests, rather than the expected time.
     *
     * @return harvest interval metric name
     */
    default String getEventHarvestIntervalMetric() {
        return MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_EVENT_HARVEST_INTERVAL;
    }

    /**
     * Returns the metric name for this service that records the expected report period. This will be the value we get back on connect.
     *
     * @return report period metric name
     */
    default String getReportPeriodInSecondsMetric() {
        return MetricNames.SUPPORTABILITY_INSIGHTS_SERVICE_REPORT_PERIOD_IN_SECONDS;
    }

    /**
     * Returns the metric name for this service that records the harvest limit. This will be the value we get back on connect.
     *
     * @return event harvest limit metric name
     */
    default String getEventHarvestLimitMetric() {
        return MetricNames.SUPPORTABILITY_CUSTOM_EVENT_DATA_HARVEST_LIMIT;
    }

    /**
     * Returns the current limit of events to store per harvest interval. For example, if the maximum size for a 60 second harvest is
     * is 10000 events and this service is configured for 5 second harvests this would return 833 (60/5 = 12, 10000/12 = 833).
     *
     * @return the maximum number of events to store per configured harvest interval
     */
    int getMaxSamplesStored();

    /**
     * Update the current limit of events to store per harvest interval.
     *
     * @param maxSamplesStored the new maximum number of events to store per harvest interval.
     */
    void setMaxSamplesStored(int maxSamplesStored);

    /**
     * Reset the event reservoir to allow for the next harvest to start
     */
    void clearReservoir();
}
