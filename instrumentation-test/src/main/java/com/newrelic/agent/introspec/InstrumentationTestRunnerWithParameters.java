/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import com.newrelic.agent.introspec.internal.ImplementationLocator;
import com.newrelic.agent.introspec.internal.IntrospectorConfig;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

import java.util.Map;

public final class InstrumentationTestRunnerWithParameters extends BlockJUnit4ClassRunnerWithParameters {

    private static Introspector INTROSPECTOR = null;
    private final Map<String, Object> configOverrides;
    private final Object[] parameters;

    public InstrumentationTestRunnerWithParameters(TestWithParameters test) throws Exception {
        super(createModifiedTest(test));
        parameters = test.getParameters().toArray(new Object[test.getParameters().size()]);
        configOverrides = IntrospectorConfig.readConfig(test.getTestClass().getJavaClass());
    }

    private static TestWithParameters createModifiedTest(TestWithParameters test) throws Exception {
        Class<?> notInstrumentedTest = test.getTestClass().getJavaClass();
        Class<?> instrumentedTest = ImplementationLocator.loadWithInstrumentingClassLoader(notInstrumentedTest, IntrospectorConfig.readConfig(notInstrumentedTest));
        return new TestWithParameters(instrumentedTest.getName(), new TestClass(instrumentedTest), test.getParameters());
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
