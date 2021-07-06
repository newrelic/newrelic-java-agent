/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec;

/**
 * Utility class to help make metric assertions in JUnit test cases run with an InstrumentationTestRunner.
 */
public class MetricsHelper {
    private MetricsHelper() {
    }

    /**
     * Get the call count for the specified metric name scoped to the specified transaction name.
     * 
     * @param transactionName transaction name
     * @param metricName metric name
     * @return call count for the specified scoped metric name
     */
    public static int getScopedMetricCount(String transactionName, String metricName) {
        TracedMetricData data;
        if (InstrumentationTestRunner.getIntrospector() != null) {
            data = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(transactionName).get(metricName);
        } else {
            data = InstrumentationTestRunnerWithParameters.getIntrospector().getMetricsForTransaction(transactionName).get(metricName);
        }
        return data == null ? 0 : data.getCallCount();
    }

    /**
     * Get the exclusive time for the specified metric name scoped to the specified transaction name.
     *
     * @param transactionName transaction name
     * @param metricName metric name
     * @return exclusive time in seconds for the specified scoped metric name
     */
    public static float getScopedMetricExclusiveTimeInSec(String transactionName, String metricName) {
        TracedMetricData data = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(
                transactionName).get(metricName);
        return data == null ? 0f : data.getExclusiveTimeInSec();
    }

    /**
     * Get the call count for the specified unscoped metric name.
     * 
     * @param metricName metric name
     * @return call count for the specified unscoped metric name
     */
    public static int getUnscopedMetricCount(String metricName) {
        TracedMetricData data = InstrumentationTestRunner.getIntrospector().getUnscopedMetrics().get(metricName);
        return data == null ? 0 : data.getCallCount();
    }
}
