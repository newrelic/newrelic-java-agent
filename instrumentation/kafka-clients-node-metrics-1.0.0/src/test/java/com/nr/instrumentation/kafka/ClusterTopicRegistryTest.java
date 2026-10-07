/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java8IncompatibleTest;
import org.apache.kafka.clients.Metadata;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

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
        FiniteMetricRecorder consumerReporter = mock(FiniteMetricRecorder.class);
        ClusterTopicRegistry consumerRegistry = new ClusterTopicRegistry(ClientType.CONSUMER, mockMetadataWithClusterIds("123678ab"), false);
        consumerRegistry.register("topic1");
        consumerRegistry.report(consumerReporter);

        //no interactions with the reporter
        verifyNoInteractions(consumerReporter);

        FiniteMetricRecorder producerReporter = mock(FiniteMetricRecorder.class);
        ClusterTopicRegistry  producerRegistry = new ClusterTopicRegistry(ClientType.PRODUCER, mockMetadataWithClusterIds("zyx22134"), false);
        producerRegistry.register("topic1");
        producerRegistry.report(producerReporter);

        //no interactions with the reporter
        verifyNoInteractions(producerReporter);

    }

    @Test
    public void clusterIdInitiallyAvailableShouldReportTopicMetrics() {
        FiniteMetricRecorder producerReporter = mock(FiniteMetricRecorder.class);
        //In this test the mockMetadata already has a clusterId available.
        ClusterTopicRegistry producerRegistry = new ClusterTopicRegistry(ClientType.PRODUCER, mockMetadataWithClusterIds("123678ab"), true);

        producerRegistry.register("topic1");
        producerRegistry.register("topic2");
        producerRegistry.report(producerReporter);

        verifyMetrics(producerReporter,
                "MessageBroker/Kafka/Cluster/123678ab/Produce/topic1",
                "MessageBroker/Kafka/Cluster/123678ab/Produce/topic2"
                );

    }

    @Test
    public void clusterIdUnavailableShouldNotReportMetrics() {
        FiniteMetricRecorder consumerReporter = mock(FiniteMetricRecorder.class);
        //In this test the mockMetadata already has a null clusterId.
        ClusterTopicRegistry consumerRegistry = new ClusterTopicRegistry(ClientType.CONSUMER, mockMetadataWithClusterIds(null), true);

        //Simulate going through two reporting cycles.
        consumerRegistry.register("topic1");
        consumerRegistry.register("topic2");
        consumerRegistry.report(consumerReporter);

        consumerRegistry.register("topic3");
        consumerRegistry.report(consumerReporter);

        verifyNoInteractions(consumerReporter);

    }

    @Test
    public void clusterIdDelayedShouldReportOldAndNewTopicMetrics() {
        FiniteMetricRecorder consumerReporter = mock(FiniteMetricRecorder.class);

        //In this test the mockMetadata already has a null clusterId.
        Metadata metadata = mockMetadataWithClusterIds(null, "123456xxy");
        ClusterTopicRegistry consumerRegistry = new ClusterTopicRegistry(ClientType.CONSUMER, metadata, true);

        //These topics are registered before the clusterId is available.
        consumerRegistry.register("topic1");
        consumerRegistry.register("topic2");
        consumerRegistry.report(consumerReporter);
        verifyNoInteractions(consumerReporter);

        //This time around, the id is found, and both the old and the new metrics should be sent.
        consumerRegistry.register("topic3");
        consumerRegistry.report(consumerReporter);
        verifyMetrics(consumerReporter,
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic1",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic2",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic3"
        );

        //reset the mock invocations counter before the next cycle.
        clearInvocations(consumerReporter);

        //The cluster id should also be used for topics registered after the id is discovered.
        consumerRegistry.register("topic4");
        consumerRegistry.report(consumerReporter);
        verifyMetrics(consumerReporter,
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic1",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic2",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic3",
                "MessageBroker/Kafka/Cluster/123456xxy/Consume/topic4");
    }

    @Test
    public void clusterIdIsFixedAfterInit() {
        //It should not be possible in practice (but is technically possible within the Kafka code) for metadata to switch cluster ids.
        // This test checks two things:
        // - First, for optimization reasons, that metadata.fetch() is called only once.
        // - Second, that we never overwrite the clusterId with new values (even if they're available on the metadata).

        FiniteMetricRecorder producerReporter = mock(FiniteMetricRecorder.class);
        Metadata metadata = mockMetadataWithClusterIds("123456xxy", "456xxcvb");
        ClusterTopicRegistry producerRegistry = new ClusterTopicRegistry(ClientType.PRODUCER, metadata, true);

        //Two topics are registered. The first report should get the cluster id; subsequent reports should reuse it.
        producerRegistry.register("topic1");
        producerRegistry.report(producerReporter);
        producerRegistry.report(producerReporter);
        producerRegistry.register("topic2");
        producerRegistry.report(producerReporter);

        //Metadata is fetched once. The original cluster id is used on every report cycle.
        verify(metadata, times(1)).fetch();
        verify(producerReporter, times(3)).recordMetric("MessageBroker/Kafka/Cluster/123456xxy/Produce/topic1", 1.0f);
        verify(producerReporter, times(1)).recordMetric("MessageBroker/Kafka/Cluster/123456xxy/Produce/topic2", 1.0f);

    }

    @Test
    public void registerNullTopicShouldBeIgnored() {
        FiniteMetricRecorder reporter = mock(FiniteMetricRecorder.class);
        ClusterTopicRegistry registry = new ClusterTopicRegistry(ClientType.CONSUMER, mockMetadataWithClusterIds("cluster123"), true);

        assertFalse(registry.register(null));
        registry.report(reporter);

        verifyNoInteractions(reporter);
    }

    @Test
    public void closeShouldClearState() {
        FiniteMetricRecorder reporter = mock(FiniteMetricRecorder.class);
        ClusterTopicRegistry registry = new ClusterTopicRegistry(ClientType.CONSUMER, mockMetadataWithClusterIds("cluster123"), true);

        //Register topics and verify they report
        registry.register("topic1");
        registry.register("topic2");
        registry.report(reporter);

        verifyMetrics(reporter,
                "MessageBroker/Kafka/Cluster/cluster123/Consume/topic1",
                "MessageBroker/Kafka/Cluster/cluster123/Consume/topic2"
        );

        //Close and verify no more metrics are reported
        clearInvocations(reporter);
        registry.close();
        registry.report(reporter);

        verifyNoInteractions(reporter);
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

    private void verifyMetrics(FiniteMetricRecorder metricRecorderMock, String ... metrics) {
        for (String metric : metrics) {
            verify(metricRecorderMock).recordMetric(eq(metric), eq(1.0f));
        }
        verifyNoMoreInteractions(metricRecorderMock);
    }
}
