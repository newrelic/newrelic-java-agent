/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.stats.StatsEngine;

/**
 * An interface for objects which need to do something before and/or after the harvest.
 */
public interface HarvestListener {

    /**
     * Called before the harvest. The service may or may not be currently connected.
     */
    void beforeHarvest(String appName, StatsEngine statsEngine);

    /**
     * Called after the harvest. The service may or may not be currently connected.
     */
    void afterHarvest(String appName);

}
