/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.annotationmatchers;

public class ClassNameAnnotationMatcher implements AnnotationMatcher {
    private final String simpleClassName;
    private final boolean fullMatch;

    public ClassNameAnnotationMatcher(String className) {
        this(className, true);
    }

    public ClassNameAnnotationMatcher(String className, boolean fullMatch) {
        super();
        if (!fullMatch && !className.endsWith(";")) {
            className = className + ";";
        }
        this.simpleClassName = className;
        this.fullMatch = fullMatch;
    }

    @Override
    public boolean matches(String annotationDesc) {
        if (fullMatch) {
            return annotationDesc.equals(simpleClassName);
        } else {
            return annotationDesc.endsWith(simpleClassName);
        }
    }

}
