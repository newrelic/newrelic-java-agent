/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Specifies class prefixes that should be examined for @Weave annotations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface InstrumentationTestConfig {

    /**
     * Include the specified set of class name prefixes to be scanned for @Weave annotations.
     * @return class name prefixes to be transformed
     */
    String[] includePrefixes() default {};

    /**
     * Name of agent configuration file
     * @return name of agent configuration file for this test
     */
    String configName() default "";
}
