/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.testrunner;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.newrelic.agent.introspec.internal.ImplementationLocator;

/**
 * A JUnit runner capable of weaving classes for test cases.
 */
public final class WeavingTestRunner extends BlockJUnit4ClassRunner {

    public WeavingTestRunner(Class<?> classUnderTest) throws InitializationError {
        super(ImplementationLocator.loadWithWeavingClassLoader(classUnderTest));
    }
}
