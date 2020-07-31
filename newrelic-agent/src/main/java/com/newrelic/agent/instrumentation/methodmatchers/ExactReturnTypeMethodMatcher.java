/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class ExactReturnTypeMethodMatcher implements MethodMatcher {

    private final Type returnType;

    public ExactReturnTypeMethodMatcher(Type returnType) {
        super();
        this.returnType = returnType;
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return Type.getReturnType(desc).equals(returnType);
    }

    @Override
    public Method[] getExactMethods() {
        return null;
    }

}
