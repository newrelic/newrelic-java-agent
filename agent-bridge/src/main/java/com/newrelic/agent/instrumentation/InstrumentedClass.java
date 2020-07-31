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
 * Marks an instrumented class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstrumentedClass {
    /**
     * @return true for classes instrumented using legacy pointcut techniques
     */
    boolean legacy() default false;

    /**
     * Some of our instrumentation adds fields and methods to a class. This marks that modification.
     * 
     * @return true if we've modified fields or methods of a class
     */
    boolean classStructureModified() default false;
}