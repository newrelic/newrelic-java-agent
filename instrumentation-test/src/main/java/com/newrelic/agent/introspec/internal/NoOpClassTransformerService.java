/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.logging.IAgentLogger;

import java.lang.instrument.Instrumentation;
import java.util.Collection;

class NoOpClassTransformerService implements ClassTransformerService {
    @Override
    public void stop() {
    }

    @Override
    public void start() {
    }

    @Override
    public boolean isStoppedOrStopping() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isStartedOrStarting() {
        return true;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "ClassTransformer";
    }

    @Override
    public IAgentLogger getLogger() {
        return Agent.LOG;
    }

    @Override
    public void retransformMatchingClassesImmediately(Class<?>[] loadedClasses, Collection<ClassMatchVisitorFactory> classMatchers) {
    }

    @Override
    public void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> classMatchers) {
    }

    @Override
    public ClassRetransformer getRemoteRetransformer() {
        return null;
    }

    @Override
    public ClassRetransformer getLocalRetransformer() {
        return null;
    }

    @Override
    public Instrumentation getExtensionInstrumentation() {
        return null;
    }

    @Override
    public InstrumentationContextManager getContextManager() {
        return null;
    }

    @Override
    public PointCutClassTransformer getClassTransformer() {
        return null;
    }

    @Override
    public void checkShutdown() {
    }

    @Override
    public boolean addTraceMatcher(ClassAndMethodMatcher matcher, String metricPrefix) {
        return false;
    }

    @Override
    public boolean addTraceMatcher(ClassAndMethodMatcher matcher, TraceDetails traceDetails) {
        return false;
    }
}
