/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.metricname;

public class SimpleMetricNameFormat implements MetricNameFormat {

    private final String metricName;
    private final String transactionSegmentName;
    private final String transactionSegmentUri;

    public SimpleMetricNameFormat(String metricName) {
        this(metricName, metricName, null);
    }

    public SimpleMetricNameFormat(String metricName, String transactionSegmentName) {
        this(metricName, transactionSegmentName, null);
    }

    public SimpleMetricNameFormat(String metricName, String transactionSegmentName, String transactionSegmentUri) {
        this.metricName = metricName;
        this.transactionSegmentName = transactionSegmentName;
        this.transactionSegmentUri = transactionSegmentUri;
    }

    @Override
    public final String getMetricName() {
        return metricName;
    }

    @Override
    public String getTransactionSegmentName() {
        return transactionSegmentName;
    }

    @Override
    public String getTransactionSegmentUri() {
        return transactionSegmentUri;
    }

}
