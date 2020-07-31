/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

/**
 * A method matcher which negates the result of the given method matcher.
 */
public final class NotMethodMatcher implements MethodMatcher {
    private MethodMatcher methodMatcher;

    public NotMethodMatcher(MethodMatcher methodMatcher) {
        super();
        this.methodMatcher = methodMatcher;
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return !methodMatcher.matches(access, name, desc, annotations);
    }

    @Override
    public Method[] getExactMethods() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((methodMatcher == null) ? 0 : methodMatcher.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NotMethodMatcher other = (NotMethodMatcher) obj;
        if (methodMatcher == null) {
            if (other.methodMatcher != null)
                return false;
        } else if (!methodMatcher.equals(other.methodMatcher))
            return false;
        return true;
    }
}
