/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.Harvestable;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.service.AbstractService;

import java.util.Collections;
import java.util.Map;

class IntrospectorHarvestService extends AbstractService implements HarvestService {

    protected IntrospectorHarvestService() {
        super(IntrospectorHarvestService.class.getName());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void startHarvest(IRPMService rpmService) {

    }

    @Override
    public void stopHarvest(IRPMService rpmService) {

    }

    @Override
    public void addHarvestListener(HarvestListener listener) {

    }

    @Override
    public void removeHarvestListener(HarvestListener listener) {

    }

    @Override
    public void harvestNow() {

    }

    @Override
    public Map<String, Object> getEventDataHarvestLimits() {
        return Collections.emptyMap();
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public void addHarvestable(Harvestable harvestable) {
    }

    @Override
    public void removeHarvestable(Harvestable harvestable) {
    }

    @Override
    public void removeHarvestablesByAppName(String appName) {

    }

}
