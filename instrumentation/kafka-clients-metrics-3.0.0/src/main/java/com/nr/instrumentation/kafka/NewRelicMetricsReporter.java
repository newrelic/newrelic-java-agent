/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.agent.bridge.AgentBridge;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.nr.instrumentation.kafka.MetricsConstants.KAFKA_METRICS_DEBUG;
import static com.nr.instrumentation.kafka.MetricsConstants.NODE_PREFIX;

public class NewRelicMetricsReporter implements MetricsReporter {


    private final Map<String, KafkaMetric> metrics = new ConcurrentHashMap<>();

    private final Map<String, NodeMetricNames> nodes;

    public NewRelicMetricsReporter() {
        this.nodes = Collections.emptyMap();
    }

    public NewRelicMetricsReporter(Set<String> nodes, Mode mode) {
        this.nodes = new ConcurrentHashMap<>(nodes.size());
        for(String node: nodes) {
            this.nodes.put(node, new NodeMetricNames(node, mode));
        }
    }

    public Map<String, KafkaMetric> getMetrics() {
        return this.metrics;
    }

    public Map<String, NodeMetricNames> getNodes() {
        return nodes;
    }

    @Override
    public void init(final List<KafkaMetric> initMetrics) {
        for (KafkaMetric kafkaMetric : initMetrics) {
            String metricGroupAndName = getMetricGroupAndName(kafkaMetric);
            if (KAFKA_METRICS_DEBUG) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "init(): {0} = {1}", metricGroupAndName, kafkaMetric.metricName());
            }
            metrics.put(metricGroupAndName, kafkaMetric);
        }
        MetricsScheduler.addMetricsReporter(this);
    }

    @Override
    public void metricChange(final KafkaMetric metric) {
        String metricGroupAndName = getMetricGroupAndName(metric);
        if (KAFKA_METRICS_DEBUG) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "metricChange(): {0} = {1}", metricGroupAndName, metric.metricName());
        }
        metrics.put(metricGroupAndName, metric);
    }

    @Override
    public void metricRemoval(final KafkaMetric metric) {
        String metricGroupAndName = getMetricGroupAndName(metric);
        if (KAFKA_METRICS_DEBUG) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "metricRemoval(): {0} = {1}", metricGroupAndName, metric.metricName());
        }
        metrics.remove(metricGroupAndName);
    }

    private String getMetricGroupAndName(final KafkaMetric metric) {
        if (metric.metricName().tags().containsKey("topic")) {
            String topic = metric.metricName().tags().get("topic");
            addTopicToNodeMetrics(topic);

            // Special case for handling topic names in metrics
            return metric.metricName().group() + "/" + topic + "/" + metric.metricName().name();
        }
        return metric.metricName().group() + "/" + metric.metricName().name();
    }

    private void addTopicToNodeMetrics(String topic) {
        for (NodeMetricNames nodeMetricNames : nodes.values()) {
            nodeMetricNames.addMetricNameForTopic(topic);
        }
    }

    @Override
    public void close() {
        MetricsScheduler.removeMetricsReporter(this);
        metrics.clear();
    }

    @Override
    public void configure(final Map<String, ?> configs) {
    }

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
    public static class NodeMetricNames {

        private final String node;
        private final Mode mode;

        private final Set<String> topics = new HashSet<>();

        private final Set<String> metricNames = new HashSet<>();
        private final Set<String> eventNames = new HashSet<>();

        public NodeMetricNames(String node, Mode mode) {
            this.node = node;
            this.mode = mode;

            String nodeMetricName = NODE_PREFIX + node;
            metricNames.add(nodeMetricName);
            eventNames.add(getEventNameForMetric(nodeMetricName));
        }

        private void addMetricNameForTopic(String topic) {
            if (!topics.contains(topic)) {
                String nodeTopicMetricName = NODE_PREFIX + node + "/" + mode.getMetricSegmentName() + "/" + topic;
                metricNames.add(nodeTopicMetricName);
                eventNames.add(getEventNameForMetric(nodeTopicMetricName));

                topics.add(topic);
            }
        }

        private String getEventNameForMetric(String metricName) {
            return metricName.replace('/', '.');
        }

        public Set<String> getMetricNames() {
            return metricNames;
        }

        public Set<String> getEventNames() {
            return eventNames;
        }
    }

    public enum Mode {
        CONSUMER("Consume"),
        PRODUCER("Produce");

        private final String metricSegmentName;

        Mode(String metricSegmentName) {
            this.metricSegmentName = metricSegmentName;
        }

        public String getMetricSegmentName() {
            return metricSegmentName;
        }
    }
}
