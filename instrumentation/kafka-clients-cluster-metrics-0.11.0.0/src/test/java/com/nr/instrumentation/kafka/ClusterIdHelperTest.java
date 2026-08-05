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

    static final class StubConsumer {
        @SuppressWarnings("unused")
        private final StubMetadata metadata;

        StubConsumer(String clusterId) {
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

    // ── fromProducer ────────────────────────────────────────────────────────

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

    // ── fromConsumer ────────────────────────────────────────────────────────

    @Test
    public void fromConsumer_returnsClusterIdWhenPresent() {
        StubConsumer consumer = new StubConsumer("cluster-consumer-123");
        assertEquals("cluster-consumer-123", ClusterIdHelper.fromConsumer(consumer));
    }

    @Test
    public void fromConsumer_returnsNullWhenNoMetadataField() {
        assertNull(ClusterIdHelper.fromConsumer(new Object()));
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

    @Test
    public void distinctClassesEachResolveCorrectlyAfterHandleCachingWarmsUp() {
        // Populates the per-class caches for StubProducer and StubConsumer independently.
        assertEquals("p-1", ClusterIdHelper.fromProducer(new StubProducer("p-1")));
        assertEquals("c-1", ClusterIdHelper.fromConsumer(new StubConsumer("c-1")));
        assertEquals("p-2", ClusterIdHelper.fromProducer(new StubProducer("p-2")));
        assertEquals("c-2", ClusterIdHelper.fromConsumer(new StubConsumer("c-2")));
    }
}
