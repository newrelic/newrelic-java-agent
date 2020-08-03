/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.stats.StatsEngine;

public class JarCollectorHarvestListener implements HarvestListener {
    private final String defaultAppName;
    private final JarCollectorService service;

    public JarCollectorHarvestListener(String defaultAppName, JarCollectorService service) {
        this.defaultAppName = defaultAppName;
        this.service = service;
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        if (!appName.equals(defaultAppName)
                || !service.isEnabled()
                || !service.isStartedOrStarting()) {
            return;
        }

        service.harvest();
    }

    @Override
    public void afterHarvest(String appName) {
    }
}
