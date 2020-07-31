/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.ConnectionConfigListener;
import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.service.AbstractService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class IntrospectorRPMServiceManager extends AbstractService implements RPMServiceManager {

    private IRPMService defaultServiceForAll;

    protected IntrospectorRPMServiceManager() {
        super(IntrospectorRPMServiceManager.class.getName());
        defaultServiceForAll = new IntrospectorRPMService();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setConnectionConfigListener(ConnectionConfigListener connectionConfigListener) {
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
    }

    @Override
    public IRPMService getRPMService() {
        return defaultServiceForAll;
    }

    @Override
    public IRPMService getRPMService(String appName) {
        return defaultServiceForAll;
    }

    @Override
    public IRPMService getOrCreateRPMService(String appName) {
        return defaultServiceForAll;
    }

    @Override
    public IRPMService getOrCreateRPMService(PriorityApplicationName appName) {
        return defaultServiceForAll;
    }

    @Override
    public List<IRPMService> getRPMServices() {
        return new ArrayList<>(Collections.singletonList(defaultServiceForAll));
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

}
