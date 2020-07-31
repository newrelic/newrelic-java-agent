/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.service.Service;
import com.newrelic.api.agent.MetricAggregator;

public interface StatsService extends Service {

    /**
     * Process the given {@link StatsWork}.
     */
    void doStatsWork(StatsWork statsWork);

    /**
     * Get a {@link StatsEngine} containing the metric data to be sent to the server in the next harvest.
     * 
     * This should only be called by the {@link HarvestService}
     */
    StatsEngine getStatsEngineForHarvest(String appName);

    MetricAggregator getMetricAggregator();

}
