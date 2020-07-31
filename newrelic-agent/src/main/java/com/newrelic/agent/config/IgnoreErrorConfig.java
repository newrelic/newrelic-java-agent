/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface IgnoreErrorConfig {

    /**
     * Returns the fully qualified class name of an exception that should be ignored. e.g:
     *
     * <pre>com.newrelic.exceptions.IgnoredException</pre>
     *
     * @return the fully qualified class name of an ignored exception
     */
    String getErrorClass();

    /**
     * Returns the optional string that represents the error message for the exception class name above.
     *
     * <b>NOTE:</b> This will return null if no message string exists.
     *
     * @return string for matching on the message of an error or null if not present
     */
    String getErrorMessage();

}