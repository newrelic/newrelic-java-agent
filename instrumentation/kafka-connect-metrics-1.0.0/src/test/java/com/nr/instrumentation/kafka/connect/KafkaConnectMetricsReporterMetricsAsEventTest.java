/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka.connect;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "org.apache.kafka", configName = "metrics_as_events.yml")
public class KafkaConnectMetricsReporterMetricsAsEventTest extends KafkaConnectMetricsReporterAbstractTest {

    private static final String SIMPLE_ATT_NAME = GROUP_NAME + "." + METRIC_1_NAME;
    private static final String CONNECTOR_ATT_NAME = GROUP_NAME + "." + CONNECTOR + "." + METRIC_2_NAME;
    private static final String TASK_ATT_NAME = GROUP_NAME + "." + CONNECTOR + "-" + TASK + "." + METRIC_3_NAME;

    private Introspector introspector;

    @Before
    public void setup() {
        introspector = InstrumentationTestRunner.getIntrospector();
    }

    @Test
    public void testMetricsAsEvents() throws InterruptedException {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                asList(SIMPLE_METRIC, TASK_METRIC),
                emptyList());

        Collection<Event> kafkaConnectMetrics = introspector.getCustomEvents("KafkaConnectMetrics");
        assertEquals(1, kafkaConnectMetrics.size());
        Event event = kafkaConnectMetrics.iterator().next();
        assertEquals(2, event.getAttributes().size());

        Set<String> eventAttrNames = new HashSet<>();
        eventAttrNames.add(SIMPLE_ATT_NAME);
        eventAttrNames.add(TASK_ATT_NAME);
        assertEquals(eventAttrNames, event.getAttributes().keySet());

        for (Object value : event.getAttributes().values()) {
            assertEquals(METRIC_VALUE.floatValue(), value);
        }

        metricsReporter.close();
    }


    @Test
    public void testChangedMetricsAsEvents() throws InterruptedException {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                emptyList(),
                asList(SIMPLE_METRIC, CONNECTOR_METRIC)
        );

        metricsReporter.harvest();

        Collection<Event> kafkaConnectMetrics = introspector.getCustomEvents("KafkaConnectMetrics");
        assertEquals(1, kafkaConnectMetrics.size());
        Event event = kafkaConnectMetrics.iterator().next();
        assertEquals(2, event.getAttributes().size());

        Set<String> eventAttrNames = new HashSet<>();
        eventAttrNames.add(SIMPLE_ATT_NAME);
        eventAttrNames.add(CONNECTOR_ATT_NAME);
        assertEquals(eventAttrNames, event.getAttributes().keySet());

        for (Object value : event.getAttributes().values()) {
            assertEquals(METRIC_VALUE.floatValue(), value);
        }

        metricsReporter.close();
    }

    @Test
    public void testUnsupportedMetricsAsEvents() throws InterruptedException {
        KafkaConnectMetricsReporter metricsReporter = initMetricsReporter(
                singletonList(UNSUPPORTED_METRIC),
                emptyList()
        );

        metricsReporter.harvest();

        Collection<Event> kafkaConnectMetrics = introspector.getCustomEvents("KafkaConnectMetrics");
        assertEquals(0, kafkaConnectMetrics.size());
        metricsReporter.close();
    }


}