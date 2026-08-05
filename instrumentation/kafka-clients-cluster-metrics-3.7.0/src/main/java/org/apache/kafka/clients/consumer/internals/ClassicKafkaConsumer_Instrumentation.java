/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer.internals;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import java.util.logging.Level;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.Utils;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

// Kafka renamed LegacyKafkaConsumer to ClassicKafkaConsumer in 3.9.0 — a minor version before
// this module's upper bound of 4.0.0 — while keeping the same metadata field and poll overloads.
// This class covers that renamed target for kafka-clients [3.9.0, 4.0.0); LegacyKafkaConsumer_Instrumentation
// in this same module covers [3.7.0, 3.9.0). At most one of the two target classes exists in any
// given Kafka release, so exactly one of the two weaves applies — the other silently doesn't match.
@Weave(originalName = "org.apache.kafka.clients.consumer.internals.ClassicKafkaConsumer")
public class ClassicKafkaConsumer_Instrumentation<K, V> {

    private final ConsumerMetadata metadata = Weaver.callOriginal();

    @NewField
    private volatile String nrClusterId;

    public ConsumerRecords<K, V> poll(final Duration timeout) {
        final ConsumerRecords<K, V> records = Weaver.callOriginal();

        if (nrClusterId == null) {
            try {
                String id = metadata.fetch().clusterResource().clusterId();
                if (id != null && !id.isEmpty()) {
                    nrClusterId = id;
                }
            } catch (Exception e) { NewRelic.getAgent().getLogger().log(Level.FINEST, e, "NR Kafka cluster ID fetch failed"); }
        }

        if (records != null && !records.isEmpty() && nrClusterId != null) {
            nrRecordClusterMetrics(records, nrClusterId);
        }

        return records;
    }

    public ConsumerRecords<K, V> poll(final long timeoutMs) {
        final ConsumerRecords<K, V> records = Weaver.callOriginal();

        if (nrClusterId == null) {
            try {
                String id = metadata.fetch().clusterResource().clusterId();
                if (id != null && !id.isEmpty()) {
                    nrClusterId = id;
                }
            } catch (Exception e) { NewRelic.getAgent().getLogger().log(Level.FINEST, e, "NR Kafka cluster ID fetch failed"); }
        }

        if (records != null && !records.isEmpty() && nrClusterId != null) {
            nrRecordClusterMetrics(records, nrClusterId);
        }

        return records;
    }

    private static void nrRecordClusterMetrics(ConsumerRecords<?, ?> records, String clusterId) {
        final Map<String, Integer> topicCounts = new HashMap<String, Integer>();
        for (ConsumerRecord<?, ?> record : records) {
            String topic = record.topic();
            Integer prev = topicCounts.get(topic);
            topicCounts.put(topic, prev == null ? 1 : prev + 1);
        }
        for (Map.Entry<String, Integer> entry : topicCounts.entrySet()) {
            NewRelic.getAgent().getMetricAggregator().recordMetric(
                    Utils.KAFKA_CLUSTER_METRIC_PREFIX + clusterId
                            + Utils.KAFKA_CLUSTER_TOPIC_SEGMENT + entry.getKey()
                            + Utils.KAFKA_CLUSTER_CONSUME_SUFFIX,
                    entry.getValue().floatValue());
        }
    }
}
