/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import com.newrelic.agent.introspec.internal.IntrospectorConfig;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;

import com.newrelic.agent.introspec.internal.ImplementationLocator;

import java.util.Map;

public final class InstrumentationTestRunner extends BlockJUnit4ClassRunner {

    private static Introspector INTROSPECTOR = null;
    private final Map<String, Object> configOverrides;

    public InstrumentationTestRunner(Class<?> classUnderTest) throws Exception {
        super(ImplementationLocator.loadWithInstrumentingClassLoader(classUnderTest, IntrospectorConfig.readConfig(classUnderTest)));
        configOverrides = IntrospectorConfig.readConfig(classUnderTest);
    }

    @Override
    public void run(RunNotifier notifier) {
        INTROSPECTOR = ImplementationLocator.createIntrospector(configOverrides);
        super.run(notifier);
        INTROSPECTOR = null;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        INTROSPECTOR.clear();
        super.runChild(method, notifier);
    }

    public static Introspector getIntrospector() {
        return INTROSPECTOR;
    }
}
