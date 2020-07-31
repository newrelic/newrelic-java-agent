/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

/**
 * An exception thrown from {@link Tracer} constructors to indicate that a tracer should be skipped.
 */
public class SkipTracerException extends RuntimeException {

}
