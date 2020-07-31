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
 * This marks classes that, if present, should cause an entire weave package to abort loading. This is used when
 * instrumenting multiple versions of a framework or library to help ensure that only one version of the instrumentation
 * ever loads.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipIfPresent {

    /**
     *
     * Canonical (package + class) name of the class that will cause a weave package not to load if present. Defaults to
     * the canonical name of the class this annotation is present on.
     */
    String originalName() default "";
}
