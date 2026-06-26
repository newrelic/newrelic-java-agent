/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.ClusterIdHelper;
import com.nr.instrumentation.kafka.Utils;
import java.time.Duration;

@Weave(originalName = "org.apache.kafka.clients.consumer.KafkaConsumer")
public class KafkaConsumer_Instrumentation<K, V> {

    @NewField
    private volatile String nrClusterId;

    @NewField
    private volatile long nrClusterIdFetchedAt;

    public ConsumerRecords<K, V> poll(final Duration timeout) {
        final ConsumerRecords<K, V> records = Weaver.callOriginal();
        if (nrClusterId == null || System.currentTimeMillis() - nrClusterIdFetchedAt > Utils.CLUSTER_ID_TTL_MS) {
            nrClusterIdFetchedAt = System.currentTimeMillis();
            String id = ClusterIdHelper.fromConsumer(this);
            if (id != null) {
                nrClusterId = id;
            }
        }
        if (records != null && !records.isEmpty() && nrClusterId != null) {
            nrRecordClusterMetrics(records, nrClusterId);
        }
        return records;
    }

    public ConsumerRecords<K, V> poll(final long timeoutMs) {
        final ConsumerRecords<K, V> records = Weaver.callOriginal();
        if (nrClusterId == null || System.currentTimeMillis() - nrClusterIdFetchedAt > Utils.CLUSTER_ID_TTL_MS) {
            nrClusterIdFetchedAt = System.currentTimeMillis();
            String id = ClusterIdHelper.fromConsumer(this);
            if (id != null) {
                nrClusterId = id;
            }
        }
        if (records != null && !records.isEmpty() && nrClusterId != null) {
            nrRecordClusterMetrics(records, nrClusterId);
        }
        return records;
    }

    private static void nrRecordClusterMetrics(ConsumerRecords<?, ?> records, String clusterId) {
        final java.util.Map<String, Integer> topicCounts = new java.util.HashMap<String, Integer>();
        for (ConsumerRecord<?, ?> record : records) {
            String topic = record.topic();
            Integer prev = topicCounts.get(topic);
            topicCounts.put(topic, prev == null ? 1 : prev + 1);
        }
        for (java.util.Map.Entry<String, Integer> entry : topicCounts.entrySet()) {
            NewRelic.getAgent().getMetricAggregator().recordMetric(
                    Utils.KAFKA_CLUSTER_METRIC_PREFIX + clusterId
                            + Utils.KAFKA_CLUSTER_TOPIC_SEGMENT + entry.getKey()
                            + Utils.KAFKA_CLUSTER_CONSUME_SUFFIX,
                    entry.getValue().floatValue());
        }
    }
}
