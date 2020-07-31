/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

/**
 * A combined class and method matcher, used to register with the {@link OptimizedClassMatcher}.
 */
public interface ClassAndMethodMatcher {
    ClassMatcher getClassMatcher();

    MethodMatcher getMethodMatcher();
}
