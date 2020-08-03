/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.module.JarCollectorService;

public class IgnoringJarCollectorService implements JarCollectorService {
    @Override
    public ClassMatchVisitorFactory getSourceVisitor() {
        return null;
    }

    @Override
    public ExtensionsLoadedListener getExtensionsLoadedListener() {
        return null;
    }

    @Override
    public void harvest() {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public IAgentLogger getLogger() {
        return Agent.LOG;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isStartedOrStarting() {
        return false;
    }

    @Override
    public boolean isStoppedOrStopping() {
        return false;
    }
}
