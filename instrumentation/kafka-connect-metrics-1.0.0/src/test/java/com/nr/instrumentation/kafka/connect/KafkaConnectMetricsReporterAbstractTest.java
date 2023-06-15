/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka.connect;

import org.apache.kafka.common.metrics.KafkaMetric;

import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Helper methods for the 2 test classes that use different values in a static final variable.
 */
public class KafkaConnectMetricsReporterAbstractTest {

    protected static final String GROUP_NAME = "group";
    protected static final String METRIC_1_NAME = "whales";
    protected static final String METRIC_2_NAME = "towels";
    protected static final String METRIC_3_NAME = "vogons";
    protected static final String CONNECTOR = "hhgg";
    protected static final String TASK = "0";
    protected static final Double METRIC_VALUE = 42.0;
    protected static final KafkaMetric SIMPLE_METRIC = getMetricMock(METRIC_1_NAME, null, null, METRIC_VALUE);
    protected static final KafkaMetric CONNECTOR_METRIC = getMetricMock(METRIC_2_NAME, CONNECTOR, null, METRIC_VALUE);
    protected static final KafkaMetric TASK_METRIC = getMetricMock(METRIC_3_NAME, CONNECTOR, TASK, METRIC_VALUE);
    protected static final KafkaMetric UNSUPPORTED_METRIC = getMetricMock("agrajag", null, null, "This value is not processed by the reporter");

    /**
     * @param initMetrics the metrics that will be available during the initial harvest
     * @param otherMetrics metrics added later by a different code path. These metrics will be available after a manual harvest.
     */
    protected static KafkaConnectMetricsReporter initMetricsReporter(List<KafkaMetric> initMetrics, Collection<KafkaMetric> otherMetrics) throws InterruptedException {
        KafkaConnectMetricsReporter metricsReporter = new KafkaConnectMetricsReporter();
        metricsReporter.init(initMetrics);
        // init triggers the first harvest that happens in a different thread. Letting it finish.
        Thread.sleep(100L);

        for (KafkaMetric otherMetric : otherMetrics) {
            metricsReporter.metricChange(otherMetric);
        }
        return metricsReporter;
    }

    protected static KafkaMetric getMetricMock(String name, String connector, String task, Object value) {
        KafkaMetric metric = mock(KafkaMetric.class, RETURNS_DEEP_STUBS);
        when(metric.metricName().group())
                .thenReturn(GROUP_NAME);
        when(metric.metricName().name())
                .thenReturn(name);
        when(metric.metricValue())
                .thenReturn(value);
        if (connector != null) {
            when(metric.metricName().tags().containsKey("connector"))
                    .thenReturn(true);
            when(metric.metricName().tags().get("connector"))
                    .thenReturn(connector);
        }
        if (task != null) {
            when(metric.metricName().tags().containsKey("task"))
                    .thenReturn(true);
            when(metric.metricName().tags().get("task"))
                    .thenReturn(task);
        }
        return metric;
    }
}