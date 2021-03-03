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
 * Sets the priority order that a weave instrumentation class should be processed in when processing all classes in a given {@code WeavePackage}.
 *
 * This can be used to force weave instrumentation classes to be processed in a particular order, if necessary.
 *
 * Usage: @WeavePriorityOrder(n)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WeavePriorityOrder {

    /**
     * The priority order. Lower numbers are considered higher in priority. Defaults to 0.
     */
    int value() default 0;

}
