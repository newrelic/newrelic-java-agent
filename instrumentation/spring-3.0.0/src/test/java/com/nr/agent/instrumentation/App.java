/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import java.util.Collections;

import com.newrelic.api.agent.Trace;

public class App {

    @Trace(dispatcher = true)
    public static void error() {
        try {
            ErrorPath path = new ErrorPath();
            path.testError();
        }
        catch( RuntimeException caught) {
            System.out.printf("Caught exception");
        }
    }

    @Trace(dispatcher = true)
    public static String pathClass() {
        return new PathClass().testPath();
    }

    @Trace(dispatcher = true)
    public static String innerPath() {
        return new TestInnerAndDefaultPath().testInnerPath();
    }

    @Trace(dispatcher = true)
    public static String methodPath() {
        return new TestPathAnnotationForMethod().testPathAnnotation();
    }

    @Trace(dispatcher = true)
    public static String kotlinDefaultParameter() {
        return new KotlinSpringClass().read(Collections.<String>emptyList(), 10);
    }
}
