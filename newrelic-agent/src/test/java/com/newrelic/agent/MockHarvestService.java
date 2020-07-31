/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.stats.StatsEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MockHarvestService extends AbstractService implements HarvestService {

    private volatile List<HarvestListener> listeners = new ArrayList<>();

    public MockHarvestService() {
        super(HarvestService.class.getSimpleName());
    }

    @Override
    public void addHarvestListener(HarvestListener listener) {
        listeners.add(listener);
    }

    public List<HarvestListener> getListeners() {
        return listeners;
    }

    @Override
    public void removeHarvestable(Harvestable harvestable) {

    }

    @Override
    public void removeHarvestablesByAppName(String appName) {

    }

    public void runHarvest(String appName, StatsEngine statsEngine) {
        for (HarvestListener listener : listeners) {
            listener.beforeHarvest(appName, statsEngine);
        }
        for (HarvestListener listener : listeners) {
            listener.afterHarvest(appName);
        }
    }

    @Override
    public void harvestNow() {
    }

    @Override
    public Map<String, Object> getEventDataHarvestLimits() {
        return Collections.emptyMap();
    }

    @Override
    public void removeHarvestListener(HarvestListener listener) {
    }

    @Override
    public void addHarvestable(Harvestable harvestable) {

    }

    @Override
    public void startHarvest(IRPMService rpmService) {
    }

    @Override
    public void stopHarvest(IRPMService rpmService) {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

}
