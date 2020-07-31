/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.objectweb.asm.commons.Method;

/**
 * A method matcher which contains other method matchers.
 */
public abstract class ManyMethodMatcher implements MethodMatcher {
    protected final Collection<MethodMatcher> methodMatchers;

    protected ManyMethodMatcher(MethodMatcher... methodMatchers) {
        this(Arrays.asList(methodMatchers));
    }

    public ManyMethodMatcher(Collection<MethodMatcher> methodMatchers) {
        this.methodMatchers = methodMatchers;
    }

    public Collection<MethodMatcher> getMethodMatchers() {
        return methodMatchers;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((methodMatchers == null) ? 0 : methodMatchers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ManyMethodMatcher other = (ManyMethodMatcher) obj;
        if (methodMatchers == null) {
            if (other.methodMatchers != null) {
                return false;
            }
        } else if (methodMatchers.size() != other.methodMatchers.size()
                || !methodMatchers.containsAll(other.methodMatchers)) {
            return false;
        }
        return true;
    }

    @Override
    public Method[] getExactMethods() {
        List<Method> methods = new ArrayList<>();
        for (MethodMatcher matcher : methodMatchers) {
            Method[] exactMethods = matcher.getExactMethods();
            if (exactMethods == null) {
                return null;
            }
            methods.addAll(Arrays.asList(exactMethods));
        }
        return methods.toArray(new Method[methods.size()]);
    }

}
