/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Set;

public interface StripExceptionConfig {
    /**
     * @return true if exception message stripping is enabled.
     */
    boolean isEnabled();

    /**
     * Get the set of exception classes whose messages are allowed through. The returned strings are the
     * value of Class.getName() for each class. For example:
     *
     * <ul>
     * <li>java.lang.NullPointerException</li>
     * <li>java.lang.IllegalStateException</li>
     * </ul>
     * @return the set of exception classes that are not stripped
     */
    Set<String> getAllowedClasses();

}
