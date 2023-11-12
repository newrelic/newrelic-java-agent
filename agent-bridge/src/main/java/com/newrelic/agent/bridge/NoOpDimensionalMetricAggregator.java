package com.newrelic.agent.bridge;

import com.newrelic.api.agent.metrics.DimensionalMetricAggregator;

import java.util.Map;

public class NoOpDimensionalMetricAggregator implements DimensionalMetricAggregator {
    public static final DimensionalMetricAggregator INSTANCE = new NoOpDimensionalMetricAggregator();

    private NoOpDimensionalMetricAggregator() {}

    @Override
    public void addToSummary(String name, Map<String, ?> attributes, double value) {
    }

    @Override
    public void incrementCounter(String name, Map<String, ?> attributes) {
    }

    @Override
    public void incrementCounter(String name, Map<String, ?> attributes, long count) {
    }
}
