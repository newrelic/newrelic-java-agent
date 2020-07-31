/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Set;

public interface ErrorCollectorConfig {

    /**
     * @return true if errors should be reported to New Relic.
     */
    boolean isEnabled();

    /**
     * @return true if error events should be reported to New Relic.
     */
    boolean isEventsEnabled();

    /**
     * @return error event reservoir size (max number of events to store per standard harvest cycle -- 60 seconds).
     */
    int getMaxSamplesStored();

    /**
     * Get the set of error classes (and optionally) error messages that occur in the application and should be ignored
     * from being reported as errors.
     *
     * @return the set of error classes and messages should be ignored
     */
    Set<IgnoreErrorConfig> getIgnoreErrors();

    /**
     * Get the set of HTTP status codes that occur in the application that should be ignored from being reported as
     * errors.
     *
     * @return the set of error codes for ignored errors
     */
    Set<Integer> getIgnoreStatusCodes();

    /**
     * Get the set of error classes (and optionally) error messages that are expected to occur in the application.
     *
     * @return the set of error classes and messages that are expected
     */
    Set<ExpectedErrorConfig> getExpectedErrors();

    /**
     * Get the set of HTTP status codes that are expected to occur in the application.
     *
     * @return the set of error codes for expected errors
     */
    Set<Integer> getExpectedStatusCodes();

    /**
     * Gets a value indicating if error priority should be ignored; default is true.
     */
    boolean isIgnoreErrorPriority();

    /**
     * Gets the defined exception handlers in configuration.
     */
    Object getExceptionHandlers();
}
