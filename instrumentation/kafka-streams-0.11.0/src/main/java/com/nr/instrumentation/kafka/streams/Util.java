/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka.streams;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.internals.StampedRecord;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class Util {

    private Util() {}

    public static void updateTransaction(StampedRecord record) {
        if (record.topic() != null) {
            MessageConsumeParameters params = MessageConsumeParameters.library("Kafka")
                    .destinationType(DestinationType.NAMED_TOPIC)
                    .destinationName(record.topic())
                    .inboundHeaders(null)
                    .build();
            NewRelic.getAgent().getTransaction().getTracedMethod().reportAsExternal(params);
            nameTransaction("MessageBroker/Kafka/Topic/Consume/Named", record.topic(), "Streams/Task");
        } else {
            nameTransaction("MessageBroker/Kafka/Streams/Task");
        }
        try {
            ConcurrentHashMapHeaders traceHeaders = ConcurrentHashMapHeaders.build(HeaderType.MESSAGE);

            ConsumerRecord<?, ?> consumerRecord = (ConsumerRecord<?, ?>)record.value();
            for (org.apache.kafka.common.header.Header header : consumerRecord.headers().headers("newrelic")) {
                traceHeaders.addHeader("newrelic", new String(header.value(), StandardCharsets.UTF_8));
            }

            NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Kafka, traceHeaders);
        } catch (ClassCastException ignored) {
            if (AgentBridge.getAgent().getLogger().isLoggable(Level.FINE)) {
                Logger logger = AgentBridge.getAgent().getLogger();
                logger.log(Level.FINE, "Failed to set distributed trace kafka headers for transaction: {0}", NewRelic.getAgent().getTransaction());
            }
        }

    }

    private static void nameTransaction(String ...parts) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "KafkaStreams", parts);
    }
}
