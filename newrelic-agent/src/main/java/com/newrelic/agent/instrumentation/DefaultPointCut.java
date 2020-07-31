/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;

public class DefaultPointCut extends PointCut {
    private final TracerFactory tracerFactory;

    public DefaultPointCut(PointCutConfiguration config, TracerFactory tracerFactory, ClassMatcher classMatcher,
            MethodMatcher methodMatcher) {
        super(config, classMatcher, methodMatcher);
        this.tracerFactory = tracerFactory;
    }

    @Override
    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return tracerFactory;
    }

    @Override
    public String toString() {
        return "DefaultPointCut:" + getClass().getName();
    }

}
