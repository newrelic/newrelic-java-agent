/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import org.apache.kafka.common.Node;
import org.apache.kafka.common.metrics.KafkaMetric;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.nr.instrumentation.kafka.MetricsConstants.NODE_PREFIX;

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
    // contains the registered metric or event names, according to config
    private final Set<String> convertedNames = ConcurrentHashMap.newKeySet();
    private final Set<String> nodes = new HashSet<>();
    private final ClientType clientType;

    public NodeTopicRegistry(ClientType clientType, Collection<Node> nodes) {
        this.clientType = clientType;
        for (Node node : nodes) {
            String nodeName = node.host() + ":" + node.port();
            this.nodes.add(nodeName);
            this.convertedNames.add(convertName(NODE_PREFIX + nodeName));
        }
    }

    /**
     * @return true if the metric contains a topic and it was registered
     */
    public boolean register(KafkaMetric metric) {
        String topic = metric.metricName().tags().get("topic");
        if (topic != null && recordedTopics.add(topic)) {
            for (String node : nodes) {
                String metricName = NODE_PREFIX + node + "/" + clientType.getOperation() + "/" + topic;
                this.convertedNames.add(convertName(metricName));
            }
            return true;
        }
        return false;
    }

    public Collection<String> getNodeTopicNames() {
        return this.convertedNames;
    }

    private String convertName(String metricName) {
        if (MetricsConstants.METRICS_AS_EVENTS) {
            return metricName.replace('/', '.');
        }
        return metricName;
    }

    public void close() {
        recordedTopics.clear();
        convertedNames.clear();
        nodes.clear();
    }
}
