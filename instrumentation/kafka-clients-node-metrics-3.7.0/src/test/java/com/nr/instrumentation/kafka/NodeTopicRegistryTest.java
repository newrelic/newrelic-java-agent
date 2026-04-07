/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import org.apache.kafka.common.Node;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NodeTopicRegistryTest {

    @Test
    public void singleNodeTest() {
        NodeTopicRegistry nodeTopicRegistry = new NodeTopicRegistry(ClientType.CONSUMER, Collections.singleton(getNode("ad")));
        assertTrue(nodeTopicRegistry.register("tm"));
        assertFalse(nodeTopicRegistry.register("tm"));
        assertTrue(nodeTopicRegistry.register("fp"));
        assertTrue(nodeTopicRegistry.register("zb"));

        FiniteMetricRecorder finiteMetricRecorder = mock(FiniteMetricRecorder.class);
        nodeTopicRegistry.report(finiteMetricRecorder);

        verifyMetrics(finiteMetricRecorder,
                "MessageBroker/Kafka/Nodes/ad:42/Consume/tm",
                "MessageBroker/Kafka/Nodes/ad:42/Consume/fp",
                "MessageBroker/Kafka/Nodes/ad:42/Consume/zb",
                "MessageBroker/Kafka/Nodes/ad:42"
                );


        // verify nothing is reported after close
        clearInvocations(finiteMetricRecorder);

        nodeTopicRegistry.close();
        nodeTopicRegistry.report(finiteMetricRecorder);

        verifyNoInteractions(finiteMetricRecorder);
    }

    @Test
    public void multiNodeTest() {
        NodeTopicRegistry nodeTopicRegistry = new NodeTopicRegistry(ClientType.PRODUCER, Arrays.asList(getNode("hh"), getNode("gg")));
        assertTrue(nodeTopicRegistry.register("vp"));
        assertFalse(nodeTopicRegistry.register("vp"));
        assertTrue(nodeTopicRegistry.register("sep"));

        FiniteMetricRecorder finiteMetricRecorder = mock(FiniteMetricRecorder.class);
        nodeTopicRegistry.report(finiteMetricRecorder);

        verifyMetrics(finiteMetricRecorder,
                "MessageBroker/Kafka/Nodes/hh:42/Produce/vp",
                "MessageBroker/Kafka/Nodes/hh:42/Produce/sep",
                "MessageBroker/Kafka/Nodes/hh:42",
                "MessageBroker/Kafka/Nodes/gg:42/Produce/vp",
                "MessageBroker/Kafka/Nodes/gg:42/Produce/sep",
                "MessageBroker/Kafka/Nodes/gg:42"
        );


        // verify nothing is reported after close
        clearInvocations(finiteMetricRecorder);

        nodeTopicRegistry.close();
        nodeTopicRegistry.report(finiteMetricRecorder);

        verifyNoInteractions(finiteMetricRecorder);
    }

    private Node getNode(String host) {
        Node node = Mockito.mock(Node.class);
        when(node.host()).thenReturn(host);
        when(node.port()).thenReturn(42);
        return node;
    }

    private void verifyMetrics(FiniteMetricRecorder metricRecorderMock, String ... metrics) {
        for (String metric : metrics) {
            verify(metricRecorderMock).recordMetric(eq(metric), eq(1.0f));
        }
        verifyNoMoreInteractions(metricRecorderMock);
    }
}