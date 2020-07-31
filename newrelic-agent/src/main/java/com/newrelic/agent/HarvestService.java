/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.service.Service;

import java.util.Map;

/**
 * The interface for the harvest service.
 */
public interface HarvestService extends Service {

    void startHarvest(IRPMService rpmService);

    void stopHarvest(IRPMService rpmService);

    void addHarvestListener(HarvestListener listener);

    void removeHarvestListener(HarvestListener listener);

    void addHarvestable(Harvestable harvestable);

    void removeHarvestable(Harvestable harvestable);

    void removeHarvestablesByAppName(String appName);

    void harvestNow();

    /**
     * Returns the object structure that will be marshaled to JSON for `event_harvest_config` that will get sent
     * up on connect. This JSON will allow for faster event harvesting by returning what the local
     * max_samples_stored value is for each event type.
     *
     * @return map of event data types to local max samples stored
     */
    Map<String, Object> getEventDataHarvestLimits();
}
