/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import org.apache.kafka.common.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to track all the metric names that are related to a specific node:
 *
 * - MessageBroker/Kafka/Nodes/host:port
 * - MessageBroker/Kafka/Nodes/host:port/Consume/topicName
 * - MessageBroker/Kafka/Nodes/host:port/Produce/topicName
 *
 * At initialization time we only have the node and the mode (is this a metrics reporter
 * for a Kafka consumer or for a Kafka producer?).
 *
 * Then, as topics are discovered through the metricChange method, the topic metric names are
 * generated. This is the best way we have to get track of the topics since they're not
 * available when the KafkaConsumer/KafkaProducer is initialized.
 *
 * For KafkaConsumer, the SubscriptionState doesn't contain the topics and partitions
 * at initialization time because it takes time for the rebalance to happen.
 *
 * For KafkaProducer, topics are dynamic since a producer could send records to any
 * topic and the concept of subscription doesn't exist there.
 *
 * Alternatively we could get the topics from the records in KafkaProducer.doSend or
 * KafkaConsumer.poll, and call NewRelicMetricsReporter.addTopicToNodeMetrics from there.
 * This approach would have a small impact in performance, and getting the topics from the
 * KafkaMetrics is a good enough solution.
 */
public class NodeTopicRegistry {
    private final Set<String> recordedTopics = ConcurrentHashMap.newKeySet();
    private final Set<String> metricNames = ConcurrentHashMap.newKeySet();
    private final Set<String> nodes = new HashSet<>();
    private final ClientType clientType;

    private static final String METRIC_PREFIX = "MessageBroker/Kafka/Nodes/";

    public NodeTopicRegistry(ClientType clientType, Collection<Node> nodes) {
        this.clientType = clientType;
        for (Node node : nodes) {
            String nodeName = node.host() + ":" + node.port();
            this.nodes.add(nodeName);
            this.metricNames.add(METRIC_PREFIX + nodeName);
        }
    }

    /**
     * @return true if the topic was registered
     */
    public boolean register(String topic) {
        if (topic != null && recordedTopics.add(topic)) {
            for (String node : nodes) {
                String metricName = METRIC_PREFIX + node + "/" + clientType.getOperation() + "/" + topic;
                this.metricNames.add(metricName);
            }
            return true;
        }
        return false;
    }

    public void report(FiniteMetricRecorder recorder) {
        for (String topicMetric : metricNames) {
            recorder.recordMetric(topicMetric, 1.0f);
        }
    }

    public void close() {
        recordedTopics.clear();
        metricNames.clear();
        nodes.clear();
    }
}
