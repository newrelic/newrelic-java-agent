package com.newrelic.api.agent;

import java.util.Map;

/**
 * This API is used to report dimensional metrics.  These metrics can be queried as follows:
 *     FROM Metric select count(some.metric.name) WHERE ...
 *
 * Metric values will either be a `count` or a `summary` depending on the API that is used.
 * If both {@link #incrementCounter(String, Map)} and {@link #addToSummary(String, Map, double)}
 * are called with the same metric name, the results will be non-deterministic.  Use one data
 * type per metric name.
 */
public interface DimensionalMetricAggregator {

    /**
     * Add a value to the summary for the given metric name.
     *
     * @param name  The name of the dimensional metric. The metric is not recorded if the name is <code>null</code> or the empty
     *              <code>String</code>.
     * @param value The value to be added to the summary.
     * @since 3.9.0
     */
    void addToSummary(String name, Map<String, Object> attributes, double value);

    /**
     * Increment the count of the metric with the given name. The count will be incremented by one each time this method
     * is called. The metric is not incremented if the name is <code>null</code> or the empty <code>String</code> .
     *
     * @param name The name of the metric to increment.
     * @since 3.9.0
     */
    void incrementCounter(String name, Map<String, Object> attributes);

    /**
     * Increment the count of the metric with the given name. The input count value represents the amount in which the
     * metric will be incremented. The metric is not incremented if the name is <code>null</code> or the empty
     * <code>String</code>.
     *
     * @param name  The name of the metric to increment.
     * @param count The amount in which the metric should be incremented.
     * @since 3.9.0
     */
    void incrementCounter(String name, Map<String, Object> attributes, int count);
}
