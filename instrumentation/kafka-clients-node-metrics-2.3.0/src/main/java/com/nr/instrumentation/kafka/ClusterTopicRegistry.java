package com.nr.instrumentation.kafka;

import org.apache.kafka.clients.Metadata;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterTopicRegistry {
    private static final String CLUSTER_METRIC_PREFIX = "MessageBroker/Kafka/Cluster/";
    private static final boolean clusterIdMetricsEnabled = true;

    private final Set<String> recordedTopics = ConcurrentHashMap.newKeySet();
    private final Set<String> clusterMetricNames = new HashSet<>();

    private Metadata metadata;
    private String clusterId;
    private final ClientType clientType;

    public ClusterTopicRegistry(ClientType clientType, Metadata metadata) {
        this.clientType = clientType;
        this.metadata = metadata;
    }

    /**
     * @return true if the topic was registered
     */
    public boolean register(String topic) {
        if (topic != null && recordedTopics.add(topic)) {
            addClusterTopic(clusterId, topic);
            return true;
        }
        return false;
    }

    public void report(FiniteMetricRecorder recorder) {
        if (clusterIdMetricsEnabled) {
            tryBuildClusterIdMetrics();
            for (String clusterMetric : clusterMetricNames) {
                recorder.recordMetric(clusterMetric, 1.0f);
            }
        }
    }

    private void tryBuildClusterIdMetrics() {
        if (clusterId == null) {
            String newClusterId = metadata.fetch().clusterResource().clusterId();
            if (newClusterId != null) {
                System.out.println("CLUSTER DEBUG: Found new cluster id: " + newClusterId);
                clusterId = newClusterId;
                for (String topic : recordedTopics) {
                    addClusterTopic(clusterId, topic);
                }
            }
        }
    }

    private void addClusterTopic(String clusterId, String topic) {
        if (clusterId != null && clusterIdMetricsEnabled) {
            String clusterMetricName = CLUSTER_METRIC_PREFIX + clusterId + "/" + clientType.getOperation() + "/" + topic;
            this.clusterMetricNames.add(clusterMetricName);
        }
    }
}
