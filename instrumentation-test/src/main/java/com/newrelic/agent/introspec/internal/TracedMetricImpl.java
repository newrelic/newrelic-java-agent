/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.stats.CountStats;

class TracedMetricImpl implements TracedMetricData {

    private final String metricName;
    private final int callCount;
    private final float totalTimeSec;
    private final float exclusiveTimeSec;

    private TracedMetricImpl(String pName, int count, float total, float exclusive) {
        metricName = pName;
        callCount = count;
        totalTimeSec = total;
        exclusiveTimeSec = exclusive;
    }

    public static TracedMetricImpl getTracedMetricFromStatsBase(String name, CountStats impl) {
        TracedMetricImpl data = new TracedMetricImpl(name, impl.getCallCount(), impl.getTotal(),
                impl.getTotalExclusiveTime());
        return data;
    }

    @Override
    public int getCallCount() {
        return callCount;
    }

    @Override
    public float getTotalTimeInSec() {
        return totalTimeSec;
    }

    @Override
    public float getExclusiveTimeInSec() {
        return exclusiveTimeSec;
    }

    @Override
    public String getName() {
        return metricName;
    }

}
