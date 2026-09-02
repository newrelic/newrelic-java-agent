/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;
import org.apache.kafka.clients.Metadata;
import org.apache.kafka.common.metrics.KafkaMetric;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.nr.instrumentation.kafka.MetricsConstants.METRICS_AS_EVENTS;

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
    private final Set<String> convertedNames = ConcurrentHashMap.newKeySet();

    private final ClientType clientType;
    private final Metadata metadata;
    private volatile String clusterId;

    public ClusterTopicRegistry(ClientType clientType, Metadata metadata, boolean clusterIdMetricsEnabled) {
        this.clientType = clientType;
        this.metadata = metadata;
        this.clusterIdMetricsEnabled = clusterIdMetricsEnabled;
    }

    /**
     * Extract the topic from the metric and save it in the list of registered topics.
     * Similar to NodeTopicRegistry.register(KafkaMetric).
     * <p>
     * If clusterIdMetrics are disabled, this is a no-op.
     * @return true if the metric contains a topic and it was registered.
     */
    public boolean register(KafkaMetric metric) {
        if (!clusterIdMetricsEnabled) {
            return false;
        }
        String topic = metric.metricName().tags().get("topic");
        if (topic != null && recordedTopics.add(topic)) {
            addClusterTopic(topic);
            return true;
        }
        return false;
    }

    /**
     * Returns the collection of cluster metric names, initializing the cluster ID if needed.
     * Similar to NodeTopicRegistry.getNodeTopicNames().
     */
    public Collection<String> getClusterMetricNames() {
        if (clusterIdMetricsEnabled && clusterId == null) {
            initializeClusterId();
        }
        return convertedNames;
    }

    public void close() {
        recordedTopics.clear();
        convertedNames.clear();
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
            convertedNames.add(convertName(clusterMetricName));
        }
    }

    private String convertName(String metricName) {
        if (METRICS_AS_EVENTS) {
            return metricName.replace('/', '.');
        }
        return metricName;
    }
}
