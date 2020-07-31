/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

/**
 * 
 * @see WebResponse#setStatusPolicy(StatusPolicy)
 */
public interface StatusCodePolicy {

    /**
     * @param currentStatus The currrent response status code
     * @param status The latest response status code
     * 
     * @return The next status code.
     */
    int nextStatus(int currentStatus, int latestStatus);

}
