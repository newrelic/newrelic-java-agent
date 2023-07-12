/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka.connect;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka")
public class KafkaConnectMetricsReporterTest extends KafkaConnectMetricsReporterAbstractTest {

    private static final String SIMPLE_METRIC_NAME = "Kafka/Connect/" + GROUP_NAME + "/" + METRIC_1_NAME;
    private static final String CONNECTOR_METRIC_NAME = "Kafka/Connect/" + GROUP_NAME + "/" + CONNECTOR + "/" + METRIC_2_NAME;
    private static final String TASK_METRIC_NAME = "Kafka/Connect/" + GROUP_NAME + "/" + CONNECTOR + "-" + TASK + "/" + METRIC_3_NAME;
    private static final float DELTA = 1.0f;

    private Introspector introspector;

    @Before
    public void setup() {
        introspector = InstrumentationTestRunner.getIntrospector();
    }

    @Test
    public void testInitMetrics() throws InterruptedException {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                asList(SIMPLE_METRIC, CONNECTOR_METRIC),
                emptyList()
        );

        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();

        Set<String> expectedMetrics = new HashSet<>();
        expectedMetrics.add(SIMPLE_METRIC_NAME);
        expectedMetrics.add(CONNECTOR_METRIC_NAME);
        assertEquals(expectedMetrics, unscopedMetrics.keySet());

        for (Map.Entry<String, TracedMetricData> metric : unscopedMetrics.entrySet()) {
            assertEquals(METRIC_VALUE.floatValue(), metric.getValue().getTotalTimeInSec(), DELTA);
        }

        metricsReporter.close();
    }

    @Test
    public void testChangeMetrics() throws InterruptedException {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                emptyList(),
                asList(SIMPLE_METRIC, TASK_METRIC)
        );

        metricsReporter.harvest();

        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();

        Set<String> expectedMetrics = new HashSet<>();
        expectedMetrics.add(SIMPLE_METRIC_NAME);
        expectedMetrics.add(TASK_METRIC_NAME);
        assertEquals(expectedMetrics, unscopedMetrics.keySet());

        for (Map.Entry<String, TracedMetricData> metric : unscopedMetrics.entrySet()) {
            assertEquals(METRIC_VALUE.floatValue(), metric.getValue().getTotalTimeInSec(), DELTA);
        }

        metricsReporter.close();
    }

    @Test
    public void testMetricsAreRemoved() throws Exception {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                asList(SIMPLE_METRIC, CONNECTOR_METRIC),
                emptyList()
        );

        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertEquals(2, unscopedMetrics.size());

        metricsReporter.metricRemoval(SIMPLE_METRIC);
        introspector.clear();
        metricsReporter.harvest();

        unscopedMetrics = introspector.getUnscopedMetrics();
        Set<String> expectedMetrics = new HashSet<>();
        expectedMetrics.add(CONNECTOR_METRIC_NAME);
        assertEquals(expectedMetrics, unscopedMetrics.keySet());

        metricsReporter.close();
    }

    @Test
    public void testUnsupportedMetricsAsEvents() throws InterruptedException {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                singletonList(UNSUPPORTED_METRIC),
                emptyList()
        );

        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertEquals(0, unscopedMetrics.size());
        metricsReporter.close();
    }

    @Test
    public void testClose() throws Exception {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                singletonList(SIMPLE_METRIC),
                emptyList()
        );

        metricsReporter.close();

        // checking side effects of close
        Map<String, KafkaMetric> metrics = getField(metricsReporter, "metrics");
        assertTrue(metrics.isEmpty());

        ScheduledFuture<?> scheduledFuture = getField(metricsReporter, "scheduledFuture");
        assertTrue(scheduledFuture.isCancelled());
    }

    private <T> T getField(Object target, String fieldName) throws Exception {
        Field field = KafkaConnectMetricsReporter.class.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return (T) field.get(target);
    }
}