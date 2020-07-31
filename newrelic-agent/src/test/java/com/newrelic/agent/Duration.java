/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

public class Duration {

    public long startTime;
    public long endTime;

    public Duration(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
