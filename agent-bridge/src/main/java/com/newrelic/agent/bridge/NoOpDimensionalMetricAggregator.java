package com.newrelic.agent.bridge;

import com.newrelic.api.agent.DimensionalMetricAggregator;

import java.util.Map;

public class NoOpDimensionalMetricAggregator implements DimensionalMetricAggregator {
    public static final DimensionalMetricAggregator INSTANCE = new NoOpDimensionalMetricAggregator();

    private NoOpDimensionalMetricAggregator() {}

    @Override
    public void addToSummary(String name, Map<String, Object> attributes, double value) {
    }

    @Override
    public void incrementCounter(String name, Map<String, Object> attributes) {
    }

    @Override
    public void incrementCounter(String name, Map<String, Object> attributes, int count) {
    }
}
