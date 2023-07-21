package com.newrelic.agent.tracers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MetricNameFormatWithHostTest {
    @Test
    public void getHost_returnsSuppliedHost() {
        MetricNameFormatWithHost metricNameFormatWithHost = MetricNameFormatWithHost.create("host", "lib");
        assertEquals("host", metricNameFormatWithHost.getHost());
    }

    @Test
    public void getMetricName_returnsMetricName() {
        MetricNameFormatWithHost metricNameFormatWithHost = MetricNameFormatWithHost.create("host", "lib");
        assertEquals("External/host/lib", metricNameFormatWithHost.getMetricName());
    }

    @Test
    public void getTransactionSegmentName_returnsMetricName() {
        MetricNameFormatWithHost metricNameFormatWithHost = MetricNameFormatWithHost.create("host", "lib");
        assertEquals("External/host/lib", metricNameFormatWithHost.getTransactionSegmentName());
    }

    @Test
    public void getTransactionSegmentUri_returnsNull() {
        MetricNameFormatWithHost metricNameFormatWithHost = MetricNameFormatWithHost.create("host", "lib");
        assertNull(metricNameFormatWithHost.getTransactionSegmentUri());
    }
}
