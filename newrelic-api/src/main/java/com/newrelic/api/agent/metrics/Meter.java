package com.newrelic.api.agent.metrics;

public interface Meter {
    Counter newCounter(String name);
    Summary newSummary(String name);
}
