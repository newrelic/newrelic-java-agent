/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractService implements Service {

    protected final IAgentLogger logger;
    private final String name;
    private final State state = new State();

    protected AbstractService(String name) {
        this.name = name;
        logger = Agent.LOG.getChildLogger(getClass());
        ServiceTiming.addServiceInitialization(name);
    }

    @Override
    public IAgentLogger getLogger() {
        return logger;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public boolean isStartedOrStarting() {
        return state.isStartedOrStarting();
    }

    @Override
    public boolean isStoppedOrStopping() {
        return state.isStoppedOrStopping();
    }

    @Override
    public boolean isStarted() {
        return state.isStarted();
    }

    @Override
    public boolean isStopped() {
        return state.isStopped();
    }

    @Override
    public final void start() throws Exception {
        if (state.beginStart()) {
            getLogger().fine(MessageFormat.format("Starting service {0}", name));
            ServiceTiming.addServiceStart(name);
            doStart();
            state.endStart();
        }
    }

    protected abstract void doStart() throws Exception;

    @Override
    public final void stop() throws Exception {
        if (state.beginStop()) {
            getLogger().fine(MessageFormat.format("Stopping service {0}", name));
            doStop();
            state.endStop();
        }
    }

    protected abstract void doStop() throws Exception;

    private static final class State {

        private AtomicReference<ServiceState> serviceState = new AtomicReference<>(ServiceState.STOPPED);

        private boolean beginStart() {
            return serviceState.compareAndSet(ServiceState.STOPPED, ServiceState.STARTING);
        }

        private void endStart() {
            serviceState.set(ServiceState.STARTED);
        }

        private boolean beginStop() {
            return serviceState.compareAndSet(ServiceState.STARTED, ServiceState.STOPPING);
        }

        private void endStop() {
            serviceState.set(ServiceState.STOPPED);
        }

        private boolean isStarted() {
            return serviceState.get() == ServiceState.STARTED;
        }

        private boolean isStartedOrStarting() {
            ServiceState state = serviceState.get();
            return state == ServiceState.STARTED || state == ServiceState.STARTING;
        }

        private boolean isStoppedOrStopping() {
            ServiceState state = serviceState.get();
            return state == ServiceState.STOPPED || state == ServiceState.STOPPING;
        }

        private boolean isStopped() {
            return serviceState.get() == ServiceState.STOPPED;
        }

    }

}
