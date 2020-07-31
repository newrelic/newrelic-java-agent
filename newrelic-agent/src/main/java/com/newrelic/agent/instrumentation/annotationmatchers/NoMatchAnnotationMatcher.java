/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.annotationmatchers;

public class NoMatchAnnotationMatcher implements AnnotationMatcher {

    @Override
    public boolean matches(String annotationDesc) {
        return false;
    }

}
