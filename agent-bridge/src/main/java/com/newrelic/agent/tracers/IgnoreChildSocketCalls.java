/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

/**
 * A marker for tracers that do not want their socket calls to be captured as external calls.
 */
public interface IgnoreChildSocketCalls {

}
