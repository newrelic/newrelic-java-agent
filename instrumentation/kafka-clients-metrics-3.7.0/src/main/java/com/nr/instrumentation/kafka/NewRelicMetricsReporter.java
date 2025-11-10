/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.agent.bridge.AgentBridge;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.nr.instrumentation.kafka.MetricsConstants.KAFKA_METRICS_DEBUG;

public class NewRelicMetricsReporter implements MetricsReporter {


    private final Map<String, KafkaMetric> metrics = new ConcurrentHashMap<>();

    private final NodeTopicRegistry nodeTopicRegistry;

    public NewRelicMetricsReporter(ClientType clientType, Collection<Node> nodes) {
        this.nodeTopicRegistry = new NodeTopicRegistry(clientType, nodes);
    }

    public Map<String, KafkaMetric> getMetrics() {
        return this.metrics;
    }

    public Collection<String> getNodeTopicNames() {
        return this.nodeTopicRegistry.getNodeTopicNames();
    }

    @Override
    public void init(final List<KafkaMetric> initMetrics) {
        for (KafkaMetric kafkaMetric : initMetrics) {
            String metricGroupAndName = getMetricGroupAndName(kafkaMetric);
            if (KAFKA_METRICS_DEBUG) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "init(): {0} = {1}", metricGroupAndName, kafkaMetric.metricName());
            }
            metrics.put(metricGroupAndName, kafkaMetric);
            nodeTopicRegistry.register(kafkaMetric);
        }
        MetricsScheduler.addMetricsReporter(this);
    }

    @Override
    public void metricChange(final KafkaMetric metric) {
        String metricGroupAndName = getMetricGroupAndName(metric);
        nodeTopicRegistry.register(metric);
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
            // Special case for handling topic names in metrics
            return metric.metricName().group() + "/" + topic + "/" + metric.metricName().name();
        }
        return metric.metricName().group() + "/" + metric.metricName().name();
    }

    @Override
    public void close() {
        MetricsScheduler.removeMetricsReporter(this);
        metrics.clear();
        nodeTopicRegistry.close();
    }

    @Override
    public void configure(final Map<String, ?> configs) {
    }
}
