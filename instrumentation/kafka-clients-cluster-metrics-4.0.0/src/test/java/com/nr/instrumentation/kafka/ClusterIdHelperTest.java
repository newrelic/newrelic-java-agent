/*
 * Copyright 2025 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.nr.instrumentation.kafka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ClusterIdHelperTest {

    // ── Test stubs simulating Kafka's internal metadata.fetch().clusterResource().clusterId() chain ──
    // Only the producer path uses ClusterIdHelper in this module — the consumer path is handled by
    // the Weaver-typed LegacyKafkaConsumer_Instrumentation field reference instead.

    static final class StubClusterResource {
        private final String clusterId;

        StubClusterResource(String clusterId) {
            this.clusterId = clusterId;
        }

        public String clusterId() {
            return clusterId;
        }
    }

    static final class StubCluster {
        private final StubClusterResource resource;

        StubCluster(String clusterId) {
            this.resource = new StubClusterResource(clusterId);
        }

        public StubClusterResource clusterResource() {
            return resource;
        }
    }

    static final class StubMetadata {
        private final StubCluster cluster;

        StubMetadata(String clusterId) {
            this.cluster = new StubCluster(clusterId);
        }

        public StubCluster fetch() {
            return cluster;
        }
    }

    static final class StubProducer {
        @SuppressWarnings("unused")
        private final StubMetadata metadata;

        StubProducer(String clusterId) {
            this.metadata = new StubMetadata(clusterId);
        }
    }

    static final class StubProducerWithNullMetadata {
        @SuppressWarnings("unused")
        private final StubMetadata metadata = null;
    }

    static final class StubProducerNoMetadataField {
        @SuppressWarnings("unused")
        private final Object somethingElse = new Object();
    }

    @Test
    public void fromProducer_returnsClusterIdWhenPresent() {
        StubProducer producer = new StubProducer("cluster-prod-123");
        assertEquals("cluster-prod-123", ClusterIdHelper.fromProducer(producer));
    }

    @Test
    public void fromProducer_returnsNullWhenNoMetadataField() {
        assertNull(ClusterIdHelper.fromProducer(new StubProducerNoMetadataField()));
    }

    @Test
    public void fromProducer_repeatedCallsOnClassWithNoMetadataFieldStayConsistent() {
        // The "field not found" result is cached via a sentinel (not null, since
        // ConcurrentHashMap.computeIfAbsent never stores a null mapping) — repeated
        // calls on the same class must keep returning null, not misbehave once cached.
        StubProducerNoMetadataField first = new StubProducerNoMetadataField();
        StubProducerNoMetadataField second = new StubProducerNoMetadataField();
        assertNull(ClusterIdHelper.fromProducer(first));
        assertNull(ClusterIdHelper.fromProducer(second));
        assertNull(ClusterIdHelper.fromProducer(first));
    }

    @Test
    public void fromProducer_returnsNullWhenMetadataFieldIsNull() {
        assertNull(ClusterIdHelper.fromProducer(new StubProducerWithNullMetadata()));
    }

    @Test
    public void fromProducer_returnsNullForEmptyClusterId() {
        StubProducer producer = new StubProducer("");
        assertNull(ClusterIdHelper.fromProducer(producer));
    }

    @Test
    public void fromProducer_returnsNullForNullClusterId() {
        StubProducer producer = new StubProducer(null);
        assertNull(ClusterIdHelper.fromProducer(producer));
    }

    @Test
    public void fromProducer_doesNotThrowOnUnexpectedInput() {
        assertNull(ClusterIdHelper.fromProducer(new Object()));
    }

    // ── Reflective handle caching (per-class, not per-value) ────────────────

    @Test
    public void repeatedCallsOnSameClassReuseCachedHandlesAndReadFreshValues() {
        // First call resolves and caches the Field/Method handles for StubProducer.
        StubProducer first = new StubProducer("cluster-v1");
        assertEquals("cluster-v1", ClusterIdHelper.fromProducer(first));

        // A second, distinct instance of the same class must still read its own,
        // independently fresh value — caching the reflective handles must not
        // accidentally cache (or cross-contaminate) the resolved value itself.
        StubProducer second = new StubProducer("cluster-v2");
        assertEquals("cluster-v2", ClusterIdHelper.fromProducer(second));

        // And the first instance must still report its own value unchanged.
        assertEquals("cluster-v1", ClusterIdHelper.fromProducer(first));
    }
}
