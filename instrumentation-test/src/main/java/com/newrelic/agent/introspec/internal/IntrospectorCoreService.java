/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.agentcontrol.HealthDataChangeListener;
import com.newrelic.agent.agentcontrol.HealthDataProducer;

class IntrospectorCoreService extends AbstractService implements CoreService, HealthDataProducer {
    private InstrumentationProxy instrumentation = null;

    public IntrospectorCoreService() {
        super(CoreService.class.getSimpleName());
    }

    public int getShutdownCount() {
        return 0;
    }

    @Override
    public InstrumentationProxy getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(InstrumentationProxy instrumentationProxy) {
        instrumentation = instrumentationProxy;
    }

    @Override
    public void shutdownAsync() {
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void registerHealthDataChangeListener(HealthDataChangeListener listener) {
    }
}
