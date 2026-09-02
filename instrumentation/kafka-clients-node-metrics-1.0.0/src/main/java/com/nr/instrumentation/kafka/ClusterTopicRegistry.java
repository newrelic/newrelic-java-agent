/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.clients.Metadata;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Utility class for building cluster-id based topic metric names for Kafka clients. Added for kafka-clients v4.0.0 and backported to earlier instrumentation.
 * <p>
 * This class reports metrics of the form:
 * - MessageBroker/Kafka/Cluster/{cluster_id}/Producer/{topic_name}
 * - MessageBroker/Kafka/Cluster/{cluster_id}/Consumer/{topic_name}
 * <p>
 * These metrics exist to supplement the similar `MessageBroker/Kafka/Nodes/{broker_address}/Producer | Consumer/{topic_name} metrics built in the
 * NodeTopicRegistry class. They are disabled by default behind the opt-in configuration:
 * <p>
 * kafka.metrics.cluster.metrics.enabled=true
 * <p>
 * The cluster id is the UUID of the cluster, retrieved from the client's metadata. The cluster id will generally **not** be available when this reporter
 * is initialized. It is expected to be fixed for the lifetime of the client, so this class attempts to fetch the id on every report cycle
 * until the first successful fetch (after which the cluster id is saved and reused).
 * <p>
 * All discovered topics for the client will be registered here. Topics may be registered before the cluster id is initialized. To account for this,
 * topic metrics are added EITHER immediately on .register (if the cluster id has already been initialized) OR during the first successful
 * .initializeClusterId().
 */

public class ClusterTopicRegistry {
    private static final String CLUSTER_METRIC_FORMAT = "MessageBroker/Kafka/Cluster/{0}/{1}/{2}"; // {clusterId}/{clientType}/{topicName}
    private final boolean clusterIdMetricsEnabled;

    private final Set<String> recordedTopics = ConcurrentHashMap.newKeySet();
    private final Set<String> clusterMetricNames = ConcurrentHashMap.newKeySet();

    private final ClientType clientType;
    private final Metadata metadata;
    private volatile String clusterId;

    public ClusterTopicRegistry(ClientType clientType, Metadata metadata, boolean clusterIdMetricsEnabled) {
        this.clientType = clientType;
        this.metadata = metadata;
        this.clusterIdMetricsEnabled = clusterIdMetricsEnabled;
    }

    /**
     * Save the topic name in the list of registered topics.
     * <p>
     * If clusterIdMetrics are disabled, this is a no-op.
     * @return true if the topic was registered.
     */
    public boolean register(String topic) {
        if (clusterIdMetricsEnabled && topic != null && recordedTopics.add(topic)) {
            addClusterTopic(topic);
            return true;
        }
        return false;
    }

    /**
     * Records a metric for each cluster * client * topic group discovered.
     * <p>
     * If clusterIdMetrics are disabled, this is a no-op.
     */
    public void report(FiniteMetricRecorder recorder) {
        if (clusterIdMetricsEnabled) {
            if (clusterId == null) {
                initializeClusterId();
            }
            for (String clusterMetric : clusterMetricNames) {
                recorder.recordMetric(clusterMetric, 1.0f);
            }
        }
    }

    public void close() {
        recordedTopics.clear();
        clusterMetricNames.clear();
        clusterId = null;
    }

    /**
     * Fetch the cluster id. If the fetch succeeds, save it and build any metrics for topics we have cached so far.
     */
    private void initializeClusterId() {
        String newClusterId = metadata.fetch().clusterResource().clusterId();
        if (newClusterId != null) {
            clusterId = newClusterId;
            for (String topic : recordedTopics) {
                addClusterTopic(topic);
            }
            NewRelic.getAgent().getLogger().log(Level.FINE, "Found cluster id: {0} for client: {1}", clusterId, clientType.name());
        }
    }

    private void addClusterTopic(String topic) {
        if (clusterId != null && topic != null) {
            String clusterMetricName = MessageFormat.format(CLUSTER_METRIC_FORMAT, clusterId, clientType.getOperation(), topic);
            clusterMetricNames.add(clusterMetricName);
        }
    }
}
