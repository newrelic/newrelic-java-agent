package com.newrelic.agent.stats.dimensional;

interface Measure {
    default void incrementCount(int count) {}

    default void addToSummary(double value) {}
}
