/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

public class NameMethodMatcher implements MethodMatcher {
    private final String name;

    public NameMethodMatcher(String name) {
        super();
        this.name = name;
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return this.name.equals(name);
    }

    @Override
    public String toString() {
        return "Match " + name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        NameMethodMatcher other = (NameMethodMatcher) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public Method[] getExactMethods() {
        return null;
    }

}
