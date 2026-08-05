/*
 * Copyright 2025 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
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
            final String clusterId = nrClusterId;
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

        return records;
    }
}
