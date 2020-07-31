/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

public final class NoMethodsMatcher implements MethodMatcher {

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return false;
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
        return true;
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
