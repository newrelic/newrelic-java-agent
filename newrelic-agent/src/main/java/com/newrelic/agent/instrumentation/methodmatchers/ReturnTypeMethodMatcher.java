/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.Set;

/**
 * A method matcher that matches methods based on return type.
 */
public final class ReturnTypeMethodMatcher implements MethodMatcher {

    private final List<String> traceReturnTypeDescriptors;

    public ReturnTypeMethodMatcher(List<String> traceReturnTypeDescriptors) {
        super();
        this.traceReturnTypeDescriptors = traceReturnTypeDescriptors;
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {

        return isTracedMethod(desc);
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

    private boolean isTracedMethod(String descriptor) {
        boolean matchFound = false;
        for(String returnTypeDescriptor: traceReturnTypeDescriptors) {
            if(descriptor.endsWith(returnTypeDescriptor)) {
                matchFound = true;
                break;
            }
        }
        return matchFound;
    }
}
