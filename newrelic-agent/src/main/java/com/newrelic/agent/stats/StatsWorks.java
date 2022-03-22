/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

public class StatsWorks {
    private StatsWorks() {
    }

    public static StatsWork getIncrementCounterWork(String name, int count) {
        return new IncrementCounter(name, count);
    }

    public static StatsWork getRecordMetricWork(String name, float value) {
        return new RecordMetric(name, value);
    }

    public static StatsWork getRecordDataUsageMetricWork(String name, long bytesSent, long bytesReceived) {
        return new RecordDataUsageMetric(name, bytesSent, bytesReceived);
    }

    public static StatsWork getRecordResponseTimeWork(String name, long millis) {
        return new RecordResponseTimeMetric(millis, name, TimeUnit.MILLISECONDS);
    }
}
