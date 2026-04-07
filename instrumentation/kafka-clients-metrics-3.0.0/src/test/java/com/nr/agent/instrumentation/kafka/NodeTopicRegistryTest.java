/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.kafka;

import com.nr.instrumentation.kafka.ClientType;
import com.nr.instrumentation.kafka.NodeTopicRegistry;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.metrics.Gauge;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeTopicRegistryTest {

    @Test
    public void singleNodeTest() {
        NodeTopicRegistry nodeTopicRegistry = new NodeTopicRegistry(ClientType.CONSUMER, Collections.singleton(getNode("ad")));
        assertTrue(nodeTopicRegistry.register(kafkaMetric("tm")));
        assertFalse(nodeTopicRegistry.register(kafkaMetric("tm")));
        assertTrue(nodeTopicRegistry.register(kafkaMetric("fp")));
        assertTrue(nodeTopicRegistry.register(kafkaMetric("zb")));

        Collection<String> metricNames = nodeTopicRegistry.getNodeTopicNames();
        verifyMetrics(metricNames,
                "MessageBroker/Kafka/Nodes/ad:42/Consume/tm",
                "MessageBroker/Kafka/Nodes/ad:42/Consume/fp",
                "MessageBroker/Kafka/Nodes/ad:42/Consume/zb",
                "MessageBroker/Kafka/Nodes/ad:42"
        );

        // verify nothing is reported after close
        nodeTopicRegistry.close();
        metricNames = nodeTopicRegistry.getNodeTopicNames();
        assertTrue(metricNames.isEmpty());
    }

    @Test
    public void multiNodeTest() {
        NodeTopicRegistry nodeTopicRegistry = new NodeTopicRegistry(ClientType.PRODUCER, Arrays.asList(getNode("hh"), getNode("gg")));
        assertTrue(nodeTopicRegistry.register(kafkaMetric("vp")));
        assertFalse(nodeTopicRegistry.register(kafkaMetric("vp")));
        assertTrue(nodeTopicRegistry.register(kafkaMetric("sep")));

        Collection<String> metricNames = nodeTopicRegistry.getNodeTopicNames();
        verifyMetrics(metricNames,
                "MessageBroker/Kafka/Nodes/hh:42/Produce/vp",
                "MessageBroker/Kafka/Nodes/hh:42/Produce/sep",
                "MessageBroker/Kafka/Nodes/hh:42",
                "MessageBroker/Kafka/Nodes/gg:42/Produce/vp",
                "MessageBroker/Kafka/Nodes/gg:42/Produce/sep",
                "MessageBroker/Kafka/Nodes/gg:42"
        );

        // verify nothing is reported after close
        nodeTopicRegistry.close();
        metricNames = nodeTopicRegistry.getNodeTopicNames();
        assertTrue(metricNames.isEmpty());
    }

    private void verifyMetrics(Collection<String> actual, String ... expectedNames) {
        assertEquals(expectedNames.length, actual.size());
        for (String metricName : expectedNames) {
            assertTrue(actual.contains(metricName));
        }
    }

    private Node getNode(String host) {
        Node node = Mockito.mock(Node.class);
        when(node.host()).thenReturn(host);
        when(node.port()).thenReturn(42);
        return node;
    }

    private KafkaMetric kafkaMetric(String topic) {
        Gauge<?> valueProvider = mock(Gauge.class);
        MetricName metricName = new MetricName("name", "group", "descr", Collections.singletonMap("topic", topic));
        return new KafkaMetric(new Object(), metricName, valueProvider, null, null);
    }
}