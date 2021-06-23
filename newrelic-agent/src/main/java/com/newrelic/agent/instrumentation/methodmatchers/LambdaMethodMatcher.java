/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * A method matcher that matches lambda methods.
 */
public final class LambdaMethodMatcher implements MethodMatcher {

    private final int ACCESS_SYNTHETIC_STATIC = 4121;
    private final Pattern lambdaMethodPattern;
    private final boolean includeNonstatic;

    public LambdaMethodMatcher(String pattern, boolean includeNonstatic) {
        super();
        this.lambdaMethodPattern = Pattern.compile(pattern);
        this.includeNonstatic = includeNonstatic;
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return (includeNonstatic || access == ACCESS_SYNTHETIC_STATIC || access == Opcodes.F_NEW) && lambdaMethodPattern.matcher(name).matches();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return getClass() == obj.getClass();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Method[] getExactMethods() {
        return null;
    }
}
