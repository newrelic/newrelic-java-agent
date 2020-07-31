/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a weave instrumentation class. Instructs the agent to weave a class into another class or set of classes.
 * 
 * Weave classes can define new member variables by marking them with the {@link NewField} annotation.
 * 
 * Methods that are being weaved must call {@link Weaver#callOriginal()} exactly once to inject the original method
 * body.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Weave {
    /**
     * The type of match. Defaults to an {@link MatchType#ExactClass} match.
     */
    MatchType type() default MatchType.ExactClass;

    /**
     * 
     * Canonical (package + class) name of the class to weave into. Defaults to the canonical name of the class this
     * annotation is present on.
     */
    String originalName() default "";
}
