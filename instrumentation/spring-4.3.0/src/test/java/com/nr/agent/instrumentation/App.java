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
        } catch (RuntimeException caught) {
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
    public static String nestedValuePath() {
        return new NestedValuePath().nestedValuePath();
    }

    @Trace(dispatcher = true)
    public static String nestedPathAnnotation() {
        return new NestedPathAnnotationTest().nestedPath();
    }

    @Trace(dispatcher = true)
    public static String pathAndValue() {
        return new PathAndValueTest().pathAndValue();
    }

    @Trace(dispatcher = true)
    public static String concreteController() {
        return new ConcreteControllerTest().concreteController();
    }

    @Trace(dispatcher = true)
    public static String abstractControllerPath() {
        return new ConcreteControllerTest().abstractControllerPath();
    }

    @Trace(dispatcher = true)
    public static String abstractControllerNoPath() {
        return new ConcreteControllerTest().abstractControllerNoPath();
    }

    @Trace(dispatcher = true)
    public static String kotlinDefaultParameter() {
        return new KotlinSpringClass().read(Collections.<String>emptyList(), 10);
    }

    @Trace(dispatcher = true)
    public static String get() {
        return new VerbTests().getMapping();
    }

    @Trace(dispatcher = true)
    public static String patch() {
        return new VerbTests().patchMapping();
    }

    @Trace(dispatcher = true)
    public static String post() {
        return new VerbTests().postMapping();
    }

    @Trace(dispatcher = true)
    public static String put() {
        return new VerbTests().putMapping();
    }

    @Trace(dispatcher = true)
    public static String delete() {
        return new VerbTests().deleteMapping();
    }
}
