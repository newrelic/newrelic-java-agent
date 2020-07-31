/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be placed on classes which provide JMX metrics to report (JmxFrameworkValues) and should be
 * loaded on startup. If the jmx metrics get loaded through a point cut then do not use this annotation on the
 * JMXFramework class.
 *
 * @since Mar 6, 2013
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JmxInit {

}
