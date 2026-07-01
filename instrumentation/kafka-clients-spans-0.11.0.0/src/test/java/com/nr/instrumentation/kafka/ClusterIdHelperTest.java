/*
 * Copyright 2025 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.nr.instrumentation.kafka;

import com.nr.instrumentation.kafka.ClusterIdHelper;
import org.apache.kafka.common.ClusterResource;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ClusterIdHelper reflection-based cluster ID extraction.
 *
 * ClusterIdHelper is used by instrumentation modules targeting Kafka &lt; 3.7.
 * In those versions, both KafkaProducer and KafkaConsumer expose a {@code metadata}
 * field directly. ClusterIdHelper walks: metadata.fetch() → Cluster.clusterResource()
 * → ClusterResource.clusterId().
 *
 * For Kafka 3.7+, the spans-consumer-3.7.0 module weaves LegacyKafkaConsumer directly
 * using the Weaver field-reference pattern — it does not use ClusterIdHelper.
 *
 * Test stubs simulate the relevant internal Kafka field and method structure so the
 * reflection logic can be exercised without a real Kafka instance.
 */
public class ClusterIdHelperTest {

    // ── Test stubs simulating Kafka internal class structure ──────────────────

    /** Simulates Kafka's internal Cluster class — exposes clusterResource(). */
    static final class TestKafkaCluster {
        private final ClusterResource resource;

        TestKafkaCluster(String clusterId) {
            this.resource = new ClusterResource(clusterId);
        }

        public ClusterResource clusterResource() {
            return resource;
        }
    }

    /**
     * Simulates Kafka's internal Metadata class — exposes fetch() returning a Cluster.
     * Both producers and consumers hold a field of this type (or a subclass).
     */
    static final class TestKafkaMetadata {
        private final TestKafkaCluster cluster;

        TestKafkaMetadata(String clusterId) {
            this.cluster = new TestKafkaCluster(clusterId);
        }

        public TestKafkaCluster fetch() {
            return cluster;
        }
    }

    /** Simulates KafkaProducer internal structure: has a 'metadata' field. */
    static final class TestKafkaProducer {
        @SuppressWarnings("unused")
        private final TestKafkaMetadata metadata;

        TestKafkaProducer(String clusterId) {
            this.metadata = new TestKafkaMetadata(clusterId);
        }
    }

    /**
     * Simulates KafkaConsumer for Kafka &lt; 3.7: has a 'metadata' field directly.
     * (Kafka 3.7+ consumers are handled by a separate instrumentation module that
     *  does not use ClusterIdHelper.)
     */
    static final class TestKafkaConsumer {
        @SuppressWarnings("unused")
        private final TestKafkaMetadata metadata;

        TestKafkaConsumer(String clusterId) {
            this.metadata = new TestKafkaMetadata(clusterId);
        }
    }

    // ── fromProducer tests ────────────────────────────────────────────────────

    @Test
    public void fromProducer_returnsClusterIdWhenMetadataPresent() {
        TestKafkaProducer producer = new TestKafkaProducer("cluster-prod-123");
        String result = ClusterIdHelper.fromProducer(producer);
        assertEquals("cluster-prod-123", result);
    }

    @Test
    public void fromProducer_returnsNullWhenNoMetadataField() {
        // Object with no 'metadata' field — reflection should not crash, just return null
        String result = ClusterIdHelper.fromProducer(new Object());
        assertNull("Should return null when metadata field is absent", result);
    }

    @Test
    public void fromProducer_returnsNullForNullInput() {
        String result = ClusterIdHelper.fromProducer(null);
        assertNull("Should return null for null producer", result);
    }

    @Test
    public void fromProducer_doesNotThrowOnNullMetadata() {
        // Verifies ClusterIdHelper is defensive when metadata field is null — a crash
        // here would propagate to the doSend() weave and break all produces.
        Object producerWithNullMetadata = new Object() {
            @SuppressWarnings("unused")
            private final TestKafkaMetadata metadata = null;
        };

        String result = null;
        try {
            result = ClusterIdHelper.fromProducer(producerWithNullMetadata);
        } catch (Exception e) {
            fail("ClusterIdHelper.fromProducer should not throw on null metadata: " + e.getMessage());
        }
        assertNull("Should return null when metadata field is null", result);
    }

    // ── fromConsumer tests ────────────────────────────────────────────────────

    @Test
    public void fromConsumer_returnsClusterIdWhenMetadataPresent() {
        TestKafkaConsumer consumer = new TestKafkaConsumer("cluster-consumer-123");
        String result = ClusterIdHelper.fromConsumer(consumer);
        assertEquals("cluster-consumer-123", result);
    }

    @Test
    public void fromConsumer_returnsNullWhenNoMetadataField() {
        String result = ClusterIdHelper.fromConsumer(new Object());
        assertNull("Should return null when metadata field is absent", result);
    }

    @Test
    public void fromConsumer_returnsNullForNullInput() {
        String result = ClusterIdHelper.fromConsumer(null);
        assertNull("Should return null for null consumer", result);
    }
}
