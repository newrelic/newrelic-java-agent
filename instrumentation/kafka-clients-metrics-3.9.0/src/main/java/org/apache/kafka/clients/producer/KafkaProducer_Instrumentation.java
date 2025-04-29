/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.producer;

import com.nr.instrumentation.kafka.ClientType;
import org.apache.kafka.clients.producer.internals.ProducerMetadata;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.Node;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.CallbackWrapper;
import com.nr.instrumentation.kafka.NewRelicMetricsReporter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.List;

@Weave(originalName = "org.apache.kafka.clients.producer.KafkaProducer")
public class KafkaProducer_Instrumentation<K, V> {
    private final Metrics metrics = Weaver.callOriginal();

    private final ProducerMetadata metadata = Weaver.callOriginal();

    @NewField
    private boolean initialized;

    @WeaveAllConstructors
    public KafkaProducer_Instrumentation() {
        if (!initialized) {
            List<Node> nodes = metadata.fetch().nodes();
            metrics.addReporter(new NewRelicMetricsReporter(ClientType.PRODUCER, nodes));
            initialized = true;
        }
    }

    @Trace
    private Future<RecordMetadata> doSend(ProducerRecord record, Callback callback) {
        if (callback != null) {
            // Wrap the callback so we can capture metrics about messages being produced
            callback = new CallbackWrapper(callback, record.topic());
        }
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            // use null for headers so we don't try to do CAT
            MessageProduceParameters params = MessageProduceParameters.library("Kafka")
                    .destinationType(DestinationType.NAMED_TOPIC)
                    .destinationName(record.topic())
                    .outboundHeaders(null)
                    .build();
            NewRelic.getAgent().getTransaction().getTracedMethod().reportAsExternal(params);
        }

        try {
            return Weaver.callOriginal();
        } catch (Exception e) {
            Map<String, Object> atts = new HashMap<>();
            atts.put("topic_name", record.topic());
            NewRelic.noticeError(e, atts);
            throw e;
        }
    }
}
