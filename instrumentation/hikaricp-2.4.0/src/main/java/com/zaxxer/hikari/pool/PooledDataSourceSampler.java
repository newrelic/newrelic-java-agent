/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.zaxxer.hikari.pool;

import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.NewRelic;
import com.zaxxer.hikari.HikariConfig;

import java.lang.ref.WeakReference;

/**
 * This sampler keeps a weak reference to a BaseHikariPool and
 * reports metrics about it.
 */
public class PooledDataSourceSampler implements Runnable {

    private final WeakReference<HikariPool_Instrumentation> hikariPoolRef;
    private final WeakReference<HikariConfig> hikariConfigRef;
    private final String baseName;

    public PooledDataSourceSampler(HikariPool_Instrumentation hikariPool, HikariConfig config) {
        this.hikariPoolRef = new WeakReference<>(hikariPool);
        this.hikariConfigRef = new WeakReference<>(config);
        this.baseName = "Database Connection/HikariCP/" + config.getPoolName() + '/';
    }

    @Override
    public void run() {
        HikariPool_Instrumentation hikariPool = hikariPoolRef.get();
        HikariConfig config = hikariConfigRef.get();
        if (null == hikariPool || null == config) {
            return;
        }
               
        MetricAggregator metricAggregator = NewRelic.getAgent().getMetricAggregator();
        metricAggregator.recordMetric(baseName + "Busy Count[connections]", hikariPool.getActiveConnections());
        metricAggregator.recordMetric(baseName + "Idle Count[connections]", hikariPool.getIdleConnections());
        metricAggregator.recordMetric(baseName + "Total Count[connections]", hikariPool.getTotalConnections());
        metricAggregator.recordMetric(baseName + "Threads Awaiting Count[connections]", hikariPool.getThreadsAwaitingConnection());
        metricAggregator.recordMetric(baseName + "Max Pool Size[connections]", config.getMaximumPoolSize());
        metricAggregator.recordMetric(baseName + "Minimum Idle Size[connections]", config.getMinimumIdle());
        metricAggregator.recordMetric(baseName + "Connection Timeout", config.getConnectionTimeout());
        metricAggregator.recordMetric(baseName + "Idle Timeout", config.getIdleTimeout());
        metricAggregator.recordMetric(baseName + "Leak Detection Threshold", config.getLeakDetectionThreshold());
        metricAggregator.recordMetric(baseName + "Maximum Lifetime", config.getMaxLifetime());
        metricAggregator.recordMetric(baseName + "Validation Timeout", config.getValidationTimeout());
    }
}
