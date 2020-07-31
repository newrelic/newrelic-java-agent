/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

/**
 * StatsWork items are submitted to the {@link StatsService}.
 */
public interface StatsWork {

    void doWork(StatsEngine statsEngine);

    String getAppName();
}
