/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

/**
 * Represents an error seen by a transaction in the Java agent. Either the throwable or the response status and
 * message will be set.
 */
public interface Error {

    Throwable getThrowable();

    int getResponseStatus();

    String getErrorMessage();
}