/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.metricname;

/**
 * Returns the metric name and transaction segment name for a tracer.
 */
public interface MetricNameFormat {
    String getMetricName();

    String getTransactionSegmentName();
    
    String getTransactionSegmentUri();
}
