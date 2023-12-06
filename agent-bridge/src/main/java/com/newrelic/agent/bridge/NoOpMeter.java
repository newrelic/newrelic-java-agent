package com.newrelic.agent.bridge;

import com.newrelic.api.agent.metrics.Counter;
import com.newrelic.api.agent.metrics.Meter;
import com.newrelic.api.agent.metrics.Summary;

public class NoOpMeter implements Meter {
    static final Meter INSTANCE = new NoOpMeter();
    private static final Counter COUNTER = (value, attributes) -> {
    };
    private static final Summary SUMMARY = (value, attributes) -> {
    };

    @Override
    public Counter newCounter(String name) {
        return COUNTER;
    }

    @Override
    public Summary newSummary(String name) {
        return SUMMARY;
    }
}
