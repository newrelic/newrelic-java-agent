/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.kafka;

import com.nr.instrumentation.kafka.ClientType;
import com.nr.instrumentation.kafka.ClusterTopicRegistry;
import org.apache.kafka.clients.Metadata;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.Gauge;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ClusterTopicRegistryTest {

    @Test
    public void clusterIdMetricsDisabledReportsNothing() {
        //verify that for both client types, report operation is a No-Op.
        ClusterTopicRegistry consumerRegistry = new ClusterTopicRegistry(ClientType.CONSUMER, mockMetadataWithClusterIds("123678ab"), false);
        consumerRegistry.register(kafkaMetric("topic1"));
        Collection<String> clusterMetrics = consumerRegistry.getClusterMetricNames();

        //no metrics should be returned for the consumer
        assertTrue(clusterMetrics.isEmpty());

        ClusterTopicRegistry  producerRegistry = new ClusterTopicRegistry(ClientType.PRODUCER, mockMetadataWithClusterIds("zyx22134"), false);
        producerRegistry.register(kafkaMetric("topic1"));
        clusterMetrics = producerRegistry.getClusterMetricNames();

        //no metrics should be returned for the producer
        assertTrue(clusterMetrics.isEmpty());

    }

    @Test
    public void clusterIdInitiallyAvailableShouldReportTopicMetrics() {
        //In this test the mockMetadata already has a clusterId available.
        ClusterTopicRegistry producerRegistry = new ClusterTopicRegistry(ClientType.PRODUCER, mockMetadataWithClusterIds("123678ab"), true);

        producerRegistry.register(kafkaMetric("topic1"));
        producerRegistry.register(kafkaMetric("topic2"));
        Collection<String> clusterMetrics = producerRegistry.getClusterMetricNames();

        verifyMetrics(clusterMetrics,
                "MessageBroker/Kafka/Cluster/123678ab/Produce/topic1",
                "MessageBroker/Kafka/Cluster/123678ab/Produce/topic2"
                );

    }

    @Test
    public void clusterIdUnavailableShouldNotReportMetrics() {
        //In this test the mockMetadata already has a null clusterId.
        ClusterTopicRegistry consumerRegistry = new ClusterTopicRegistry(ClientType.CONSUMER, mockMetadataWithClusterIds(null), true);

        //Simulate going through two reporting cycles.
        consumerRegistry.register(kafkaMetric("topic1"));
        consumerRegistry.register(kafkaMetric("topic2"));
        consumerRegistry.getClusterMetricNames();

        consumerRegistry.register(kafkaMetric("topic3"));
        Collection<String> clusterMetrics = consumerRegistry.getClusterMetricNames();

        assertTrue(clusterMetrics.isEmpty());
    }

    @Test
    public void clusterIdDelayedShouldReportOldAndNewTopicMetrics() {
        //In this test the mockMetadata already has a null clusterId.
        Metadata metadata = mockMetadataWithClusterIds(null, "123456xxy");
        ClusterTopicRegistry consumerRegistry = new ClusterTopicRegistry(ClientType.CONSUMER, metadata, true);

        //These topics are registered before the clusterId is available.
        consumerRegistry.register(kafkaMetric("topic1"));
        consumerRegistry.register(kafkaMetric("topic2"));
        Collection<String> clusterMetrics = consumerRegistry.getClusterMetricNames();
        assertTrue(clusterMetrics.isEmpty());

        //This time around, the id is found, and both the old and the new metrics should be sent.
        consumerRegistry.register(kafkaMetric("topic3"));
        clusterMetrics = consumerRegistry.getClusterMetricNames();
        verifyMetrics(clusterMetrics,
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic1",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic2",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic3"
        );

        //The cluster id should also be used for topics registered after the id is discovered.
        consumerRegistry.register(kafkaMetric("topic4"));
        clusterMetrics = consumerRegistry.getClusterMetricNames();
        verifyMetrics(clusterMetrics,
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic1",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic2",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic3",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic4");
    }

    @Test
    public void clusterIdIsFixedAfterInit() {
        Metadata metadata = mockMetadataWithClusterIds("123456xxy", "456xxcvb");
        ClusterTopicRegistry producerRegistry = new ClusterTopicRegistry(ClientType.PRODUCER, metadata, true);

        //Two topics are registered. The first report should get the cluster id; subsequent reports should reuse it.
        producerRegistry.register(kafkaMetric("topic1"));
        producerRegistry.getClusterMetricNames();
        producerRegistry.getClusterMetricNames();
        producerRegistry.register(kafkaMetric("topic2"));
        Collection<String> clusterMetrics = producerRegistry.getClusterMetricNames();

        //Metadata is fetched once. The original cluster id is used on every report cycle.
        verify(metadata, times(1)).fetch();
        verifyMetrics(clusterMetrics,
                "MessageBroker/Kafka/Cluster/123456xxy/Produce/topic1",
                        "MessageBroker/Kafka/Cluster/123456xxy/Produce/topic2");
    }

    @Test
    public void closeShouldClearState() {
        ClusterTopicRegistry registry = new ClusterTopicRegistry(ClientType.CONSUMER, mockMetadataWithClusterIds("cluster123"), true);

        //Register topics and verify they report
        registry.register(kafkaMetric("topic1"));
        registry.register(kafkaMetric("topic2"));
        Collection<String> clusterMetrics = registry.getClusterMetricNames();

        verifyMetrics(clusterMetrics,
                "MessageBroker/Kafka/Cluster/cluster123/Consume/topic1",
                "MessageBroker/Kafka/Cluster/cluster123/Consume/topic2"
        );

        //Close and verify no more metrics are reported
        registry.close();
        clusterMetrics = registry.getClusterMetricNames();

        assertTrue(clusterMetrics.isEmpty());
    }

    private Metadata mockMetadataWithClusterIds(String initial, String ... clusterIds) {
        Metadata mockMetadata = mock(Metadata.class);
        Cluster mockCluster = mock(Cluster.class);
        ClusterResource mockClusterResource = mock(ClusterResource.class);

        when(mockMetadata.fetch()).thenReturn(mockCluster);
        when(mockCluster.clusterResource()).thenReturn(mockClusterResource);
        when(mockClusterResource.clusterId()).thenReturn(initial, clusterIds);

        return mockMetadata;
    }

    private void verifyMetrics(Collection<String> actual, String ... expectedNames) {
        assertEquals(expectedNames.length, actual.size());
        for (String metricName : expectedNames) {
            assertTrue(actual.contains(metricName));
        }
    }

    private KafkaMetric kafkaMetric(String topic) {
        Gauge<?> valueProvider = mock(Gauge.class);
        MetricName metricName = new MetricName("name", "group", "descr", Collections.singletonMap("topic", topic));
        return new KafkaMetric(new Object(), metricName, valueProvider, null, null);
    }
}
