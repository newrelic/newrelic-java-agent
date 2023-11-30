package com.newrelic.api.agent.metrics;

/**
 * Dimensional metric data API.
 *
 * Metrics created with this API can be queried using NRQL.
 *
 *     SELECT average(<metric_name>) FROM Metric WHERE entity.guid = <guid>
 */
public interface Meter {
    /**
     * Creates a counter with the given dimensional metric name.
     */
    Counter newCounter(String name);
    /**
     * Creates a summary with the given dimensional metric name.
     */
    Summary newSummary(String name);
}
