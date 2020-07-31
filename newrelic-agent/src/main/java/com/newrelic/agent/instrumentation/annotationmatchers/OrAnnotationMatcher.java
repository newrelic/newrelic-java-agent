/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.annotationmatchers;

import java.util.Arrays;
import java.util.Collection;

public class OrAnnotationMatcher implements AnnotationMatcher {
    private final Collection<AnnotationMatcher> matchers;

    private OrAnnotationMatcher(Collection<AnnotationMatcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean matches(String annotationDesc) {
        for (AnnotationMatcher matcher : matchers) {
            if (matcher.matches(annotationDesc)) {
                return true;
            }
        }
        return false;
    }

    public static AnnotationMatcher getOrMatcher(AnnotationMatcher... matchers) {
        if (matchers.length == 1) {
            return matchers[0];
        } else {
            return new OrAnnotationMatcher(Arrays.asList(matchers));
        }
    }
}
