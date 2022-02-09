/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.agent.metric.MetricName;

public final class RecordDataUsageMetric implements StatsWork {
    private final MetricName name;
    private final long bytesSent;
    private final long bytesReceived;

    public RecordDataUsageMetric(String name, long bytesSent, long bytesReceived) {
        this.name = MetricName.create(name);
        this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
    }

    @Override
    public void doWork(StatsEngine statsEngine) {
        statsEngine.getDataUsageStats(name).recordDataUsage(bytesSent, bytesReceived);
    }

    @Override
    public String getAppName() {
        return null;
    }

    public String getName() {
        return name.getName();
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }
}
