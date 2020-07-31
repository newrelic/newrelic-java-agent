/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

public interface TimedItem {
    /**
     * Get the duration of the method invocation in milliseconds.
     */
    long getDurationInMilliseconds();

    /**
     * Get the duration of the method invocation in nanoseconds.
     */
    long getDuration();

    /**
     * Get the duration in nanoseconds of the tracer minus the duration of all child tracers. This measures the amount
     * of time spent in the method itself or in untraced method calls.
     */
    long getExclusiveDuration();
}
