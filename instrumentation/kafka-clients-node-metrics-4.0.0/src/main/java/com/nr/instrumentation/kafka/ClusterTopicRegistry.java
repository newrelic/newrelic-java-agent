package com.nr.instrumentation.kafka;

import org.apache.kafka.clients.Metadata;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterTopicRegistry {
    private static final String CLUSTER_METRIC_FORMAT = "MessageBroker/Kafka/Cluster/{0}/{1}/{2}"; // {clusterId}/{clientType}/{topicName}
    private static final boolean clusterIdMetricsEnabled = true;

    private final Set<String> recordedTopics = ConcurrentHashMap.newKeySet();
    private final Set<String> clusterMetricNames = new HashSet<>();

    private final ClientType clientType;
    private final Metadata metadata;
    private String clusterId;

    public ClusterTopicRegistry(ClientType clientType, Metadata metadata) {
        this.clientType = clientType;
        this.metadata = metadata;
    }

    /**
     * Save the topic name in the list of registered topics.
     *
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
     *
     * If clusterIdMetrics are disabled, this is a no-op.
     * @param recorder
     */
    public void report(FiniteMetricRecorder recorder) {
        if (clusterIdMetricsEnabled) {
            initClusterMetrics();
            for (String clusterMetric : clusterMetricNames) {
                recorder.recordMetric(clusterMetric, 1.0f);
            }
        }
    }

    /**
     * If the clusterId is not yet initialized, get it, and build any metrics for saved topics.
     *
     * This is a no-op after the first call to initClusterId() succeeds.
     */
    private void initClusterMetrics() {
        if (initClusterId()) {
            for (String topic : recordedTopics) {
                addClusterTopic(topic);
            }
        }
    }

    private boolean initClusterId(){
        if (clusterId == null) {
            String newClusterId = metadata.fetch().clusterResource().clusterId();
            if (newClusterId != null) {
                clusterId = newClusterId;
                return true;
            }
        }
        return false;
    }

    private void addClusterTopic(String topic) {
        if (clusterId != null && topic != null) {
            String clusterMetricName = MessageFormat.format(CLUSTER_METRIC_FORMAT, clusterId, clientType.getOperation(), topic);
            clusterMetricNames.add(clusterMetricName);
        }
    }
}
