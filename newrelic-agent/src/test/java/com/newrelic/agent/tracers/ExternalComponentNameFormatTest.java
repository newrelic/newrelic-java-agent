/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import org.junit.Assert;

import org.junit.Test;

import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class ExternalComponentNameFormatTest {

    @Test
    public void test() {
        MetricNameFormat format = getExternalMetricFormatter(false);

        Assert.assertEquals("External/localhost/socket", format.getMetricName());
        Assert.assertEquals("External/localhost/socket/get", format.getTransactionSegmentName());
        Assert.assertEquals("http://www.example.com:8080/hithere/hi.html", format.getTransactionSegmentUri());

        format = getExternalMetricFormatter(true);

        Assert.assertEquals("External/localhost/socket/get", format.getMetricName());
        Assert.assertEquals("External/localhost/socket/get", format.getTransactionSegmentName());
        Assert.assertEquals("http://www.example.com:8080/hithere/hi.html", format.getTransactionSegmentUri());
    }

    @Test
    public void multipleOperations() {
        MetricNameFormat format = getExternalMetricFormatter(false, "http:localhost:8080/newservlet", new String[] {
                "open", "dude" });

        Assert.assertEquals("External/localhost/socket", format.getMetricName());
        Assert.assertEquals("External/localhost/socket/open/dude", format.getTransactionSegmentName());
        Assert.assertEquals("http:localhost:8080/newservlet", format.getTransactionSegmentUri());
    }

    private static MetricNameFormat getExternalMetricFormatter(boolean includeOperationInMetric) {
        return getExternalMetricFormatter(includeOperationInMetric, "http://www.example.com:8080/hithere/hi.html",
                new String[] { "get" });
    }

    private static MetricNameFormat getExternalMetricFormatter(boolean includeOperationInMetric, String uri,
            String[] operations) {
        return ExternalComponentNameFormat.create("localhost", "socket", includeOperationInMetric, uri, operations);
    }
}
