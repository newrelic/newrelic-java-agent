/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An exact method signature matcher.
 * 
 * The name of the method to be matched and its internal description are used to match method signatures.
 */
public final class ExactMethodMatcher implements MethodMatcher {
    private final String name;
    private final Set<String> descriptions;
    private final Method[] methods;

    /**
     * Matches a single method name / method description.
     * 
     * @param name
     * @param description
     */
    public ExactMethodMatcher(String name, String description) {
        this(name, new String[] { description }); // we shouldn't need this, but single arg calls are failing at runtime
    }

    public ExactMethodMatcher(String name, Collection<String> descriptions) {
        this.name = name;
        if (descriptions.isEmpty()) {
            this.descriptions = Collections.emptySet();
            methods = null;
        } else {
            this.descriptions = Collections.unmodifiableSet(new HashSet<>(descriptions));
            methods = new Method[descriptions.size()];
            String[] desc = descriptions.toArray(new String[0]);
            for (int i = 0; i < desc.length; i++) {
                methods[i] = new Method(name, desc[i]);
            }
        }
    }

    public ExactMethodMatcher(String name, String... descriptions) {
        this(name, Arrays.asList(descriptions));
    }

    String getName() {
        return name;
    }

    Set<String> getDescriptions() {
        return descriptions;
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return this.name.equals(name) && (this.descriptions.isEmpty() || this.descriptions.contains(desc));
    }

    @Override
    public String toString() {
        return "Match " + name + descriptions;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((descriptions == null) ? 0 : descriptions.hashCode());
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
        ExactMethodMatcher other = (ExactMethodMatcher) obj;
        if (descriptions == null) {
            if (other.descriptions != null) {
                return false;
            }
        } else if (!descriptions.equals(other.descriptions)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Validates the method signature(s) described by this matcher.
     * 
     * @throws InvalidMethodDescriptor
     */
    public void validate() throws InvalidMethodDescriptor {
        boolean valid = true;
        for (String methodDesc : descriptions) {
            try {
                Type[] types = Type.getArgumentTypes(methodDesc);
                for (Type t : types) {
                    if (t == null) {
                        valid = false;
                        break;
                    }
                }
            } catch (Exception e) {
                valid = false;
            }

            if (!valid) {
                throw new InvalidMethodDescriptor("Invalid method descriptor: " + methodDesc);
            }
        }
    }

    @Override
    public Method[] getExactMethods() {
        return methods;
    }

}
