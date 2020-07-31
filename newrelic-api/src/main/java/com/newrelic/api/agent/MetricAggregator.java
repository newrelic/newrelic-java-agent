/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.concurrent.TimeUnit;

/**
 * This aggregator allows metrics to be reported which can be viewed through custom dashboards.
 */
public interface MetricAggregator {

    /**
     * Records a metric that will not be shown in the breakdown of time for a transaction, but can be displayed in a
     * custom dashboard. The metric is not recorded if the name is <code>null</code> or the empty <code>String</code>.
     * 
     * @param name The name of the metric.
     * @param totalTime The total time value for the metric. This is the value that custom dashboards will display
     *        (often the average of the value).
     * @param exclusiveTime The exclusive time for this metric. This value is primarily used by the agent for
     *        transaction blame metrics, but it is exposed here for future uses.
     * @param timeUnit The time unit of the values passed into this method.
     * @since 3.9.0
     */
    void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit);

    /**
     * Record a metric value for the given name.
     * 
     * @param name The name of the metric. The metric is not recorded if the name is <code>null</code> or the empty
     *        <code>String</code>.
     * @param value The value of the metric.
     * @since 3.9.0
     */
    void recordMetric(String name, float value);

    /**
     * Record a response time in milliseconds for the given metric name.
     * 
     * @param name The name of the metric. The response time is not recorded if the name is <code>null</code> or the
     *        empty <code>String</code>.
     * @param millis The response time in milliseconds.
     * @since 3.9.0
     */
    void recordResponseTimeMetric(String name, long millis);

    /**
     * Increment the count of the metric with the given name. The count will be incremented by one each time this method
     * is called. The metric is not incremented if the name is <code>null</code> or the empty <code>String</code> .
     * 
     * @param name The name of the metric to increment.
     * @since 3.9.0
     */
    void incrementCounter(String name);

    /**
     * Increment the count of the metric with the given name. The input count value represents the amount in which the
     * metric will be incremented. The metric is not incremented if the name is <code>null</code> or the empty
     * <code>String</code>.
     * 
     * @param name The name of the metric to increment.
     * @param count The amount in which the metric should be incremented.
     * @since 3.9.0
     */
    void incrementCounter(String name, int count);
}
