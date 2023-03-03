/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an instrumented method.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface InstrumentedMethod {

    /**
     * If true, this method generates a dispatcher tracer.
     * 
     * @return
     */
    boolean dispatcher() default false;

    /**
     * The type of instrumentation that caused this method to be instrumented.
     * 
     * @return
     */
    InstrumentationType[] instrumentationTypes();

    /**
     * The name of the instrumentation.
     * 
     * @return
     */
    String[] instrumentationNames();

}