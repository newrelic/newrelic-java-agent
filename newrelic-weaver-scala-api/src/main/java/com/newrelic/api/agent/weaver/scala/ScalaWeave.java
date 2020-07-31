/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver.scala;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * Marks a weave instrumentation class written in scala. Instructs the agent to weave a class into another class or set
 * of classes.
 * 
 * Weave classes can define new member variables by marking them with the {@link NewField} annotation.
 * 
 * Methods that are being weaved must call {@link Weaver#callOriginal()} exactly once to inject the original method
 * body.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScalaWeave {
    /**
     * The type of scala match. Defaults to an {@link ScalaMatchType#ExactClass} match.
     */
    ScalaMatchType type() default ScalaMatchType.ExactClass;

    /**
     * Canonical (package + class) name of the class to weave into. Defaults to the canonical name of the class this
     * annotation is present on.
     */
    String originalName() default "";
}
