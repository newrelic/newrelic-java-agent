/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

/**
 * Used to record method invocation time.
 */
public interface ResponseTimeStats extends CountStats {

    void recordResponseTime(long responseTime, TimeUnit timeUnit);

    void recordResponseTime(long responseTime, long exclusiveTime, TimeUnit timeUnit);

    void recordResponseTime(int count, long totalTime, long minTime, long maxTime, TimeUnit unit);

    void recordResponseTimeInNanos(long responseTime, long exclusiveTime);

    void recordResponseTimeInNanos(long responseTime);
}
