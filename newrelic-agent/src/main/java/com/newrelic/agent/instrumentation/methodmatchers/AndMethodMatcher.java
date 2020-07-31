/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

/**
 * A method matcher which 'ands' a set of method matchers - if all of the child matchers match, this matcher matches,
 * otherwise it doesn't.
 */
public final class AndMethodMatcher extends ManyMethodMatcher {
    protected AndMethodMatcher(MethodMatcher... methodMatchers) {
        super(methodMatchers);
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        for (MethodMatcher matcher : methodMatchers) {
            if (!matcher.matches(access, name, desc, annotations)) {
                return false;
            }
        }
        return true;
    }

    public static final MethodMatcher getMethodMatcher(MethodMatcher... matchers) {
        if (matchers.length == 1) {
            return matchers[0];
        }
        return new AndMethodMatcher(matchers);
    }

    @Override
    public String toString() {
        return "And Match " + methodMatchers;
    }
}
