package com.newrelic.agent.stats.dimensional;

public interface Measure {
    default void incrementCount(int count) {}

    default void addToSummary(double value) {}
}
