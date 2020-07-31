/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

/**
 * This class MUST NOT define a hashcode and equals method - there are classes which expect it two instances created
 * with the same class and method matchers to be treated uniquely (!=).
 * 
 * @see HashSafeClassAndMethodMatcher
 */
public class DefaultClassAndMethodMatcher implements ClassAndMethodMatcher {
    protected final ClassMatcher classMatcher;
    protected final MethodMatcher methodMatcher;

    public DefaultClassAndMethodMatcher(ClassMatcher classMatcher, MethodMatcher methodMatcher) {
        super();
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;
    }

    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

}
