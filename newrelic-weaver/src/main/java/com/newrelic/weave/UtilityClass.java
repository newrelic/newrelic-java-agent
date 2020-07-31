/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a utility class and is added programmatically by the AgentPreprocessors
 * to any class that doesn't have a @Weave or @SkipIfPresent annotation on it.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UtilityClass {

    /**
     * The weave package name this utility class is contained in.
     * @return weave package name this utility class is contained in
     */
    String weavePackageName() default "Unknown";
}
