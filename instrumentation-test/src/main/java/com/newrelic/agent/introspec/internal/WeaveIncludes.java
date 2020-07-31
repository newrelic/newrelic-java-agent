/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import org.junit.runners.model.InitializationError;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;

class WeaveIncludes {
    private final Iterable<String> includePrefixes;
    public final String classUnderTestName;

    public WeaveIncludes(Iterable<String> includePrefixes, String classUnderTestName) {
        this.includePrefixes = includePrefixes;
        this.classUnderTestName = classUnderTestName;
    }

    public boolean isIncluded(String className) {
        if (className == null) {
            return false;
        }

        for (String prefix : includePrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static WeaveIncludes readWeaveTestConfigIncludePrefixes(Class<?> classUnderTest) throws InitializationError {

        // determine package prefixes to scan for @Weave annotations
        HashSet<String> includePrefixes = new HashSet<>();
        for (Annotation annotation : classUnderTest.getAnnotations()) {
            if (annotation instanceof InstrumentationTestConfig) {
                Collections.addAll(includePrefixes, ((InstrumentationTestConfig) annotation).includePrefixes());
            }
        }
        if (includePrefixes.isEmpty()) {
            throw new InitializationError(
                    "Must specify prefixes to scan for @Weave annotations in an @WeaveTestConfig annotation");
        }

        return new WeaveIncludes(includePrefixes, classUnderTest.getName());
    }
}
