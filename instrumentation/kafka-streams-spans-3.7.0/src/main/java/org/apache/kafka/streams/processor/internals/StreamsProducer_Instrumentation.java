/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
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

import java.util.concurrent.Future;

@Weave(originalName = "org.apache.kafka.streams.processor.internals.StreamsProducer")
public class StreamsProducer_Instrumentation {
    @Trace(leaf = true, excludeFromTransactionTrace = true)
    Future<RecordMetadata> send(final ProducerRecord<byte[], byte[]> record, final Callback callback) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("MessageBroker/Kafka/Topic/Produce/" + record.topic());
        }
        return Weaver.callOriginal();
    }

}