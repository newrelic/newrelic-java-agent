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

import com.newrelic.api.agent.Trace;

/**
 * This annotation tells the agent to wrap a method with a try/catch block so that any exceptions it throws are logged
 * but never raised to callers.
 * 
 * Right now only methods with a void return type are supported.
 * 
 * {@link Trace}d methods implicitly have this behavior. This annotation should only be used on new helper methods. A
 * common use case is to add this annotation to methods implemented in a listener so that callbacks to the listener
 * don't throw exceptions to the calling application code.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CatchAndLog {

}
