package com.newrelic.agent.stats.dimensional;

interface Measure {
    default void incrementCount(long count) {}

    default void addToSummary(double value) {}
}
