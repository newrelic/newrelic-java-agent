/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.concurrent.TimeUnit;

import com.newrelic.api.agent.MetricAggregator;

public class NoOpMetricAggregator implements MetricAggregator {
    public static final MetricAggregator INSTANCE = new NoOpMetricAggregator();

    private NoOpMetricAggregator() {
    }

    @Override
    public void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
    }

    @Override
    public void recordMetric(String name, float value) {
    }

    @Override
    public void recordResponseTimeMetric(String name, long millis) {
    }

    @Override
    public void incrementCounter(String name) {
    }

    @Override
    public void incrementCounter(String name, int count) {
    }

}
