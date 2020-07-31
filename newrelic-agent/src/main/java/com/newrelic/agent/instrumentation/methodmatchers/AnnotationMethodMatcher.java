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

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;

/**
 * This is a complete bastardization of a method matcher. It's special cased in the {@link OptimizedClassMatcher}.
 * 
 * This matcher does not cooperate with matchers like the {@link NotMethodMatcher}, {@link OrMethodMatcher}, etc. It
 * should really only be used by the custom xml stuff.
 */
public class AnnotationMethodMatcher implements MethodMatcher {

    private final Type annotationType;
    private final String annotationDesc;

    public AnnotationMethodMatcher(Type annotationType) {
        super();
        this.annotationType = annotationType;
        annotationDesc = annotationType.getDescriptor();
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        if (annotations == MethodMatcher.UNSPECIFIED_ANNOTATIONS) {
            Agent.LOG.finer("The annotation method matcher will not work if annotations aren't specified");
        }
        return annotations.contains(annotationDesc);
    }

    @Override
    public Method[] getExactMethods() {
        return null;
    }

    public Type getAnnotationType() {
        return annotationType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotationDesc == null) ? 0 : annotationDesc.hashCode());
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
        AnnotationMethodMatcher other = (AnnotationMethodMatcher) obj;
        if (annotationType == null) {
            if (other.annotationType != null)
                return false;
        } else if (!annotationType.equals(other.annotationType))
            return false;
        return true;
    }

}
