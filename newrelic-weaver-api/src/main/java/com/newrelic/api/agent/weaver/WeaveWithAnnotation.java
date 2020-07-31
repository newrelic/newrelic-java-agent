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

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface WeaveWithAnnotation {

    /**
     * The type of match. Defaults to an {@link MatchType#ExactClass} match.
     */
    MatchType type() default MatchType.ExactClass;

    /**
     * Canonical (package + class) name of the annotation require in order to weave this type
     */
    String[] annotationClasses();

}
