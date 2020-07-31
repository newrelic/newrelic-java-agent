/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

/**
 * An exception thrown from {@link TracerFactory} to indicate that a tracer should be retried.
 */
public class RetryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

}
