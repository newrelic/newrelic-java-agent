/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.kafka.streams.processor.internals;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.concurrent.Future;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.RecordCollectorImpl")
public class RecordCollectorImpl_Instrumentation {

    @Trace(leaf = true, excludeFromTransactionTrace = true)
    public <K, V> void send(final String topic,
            final K key,
            final V value,
            final Headers headers,
            final Integer partition,
            final Long timestamp,
            final Serializer<K> keySerializer,
            final Serializer<V> valueSerializer) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("MessageBroker/Kafka/Topic/Produce/" + topic);
        }
        Weaver.callOriginal();
    }

}
