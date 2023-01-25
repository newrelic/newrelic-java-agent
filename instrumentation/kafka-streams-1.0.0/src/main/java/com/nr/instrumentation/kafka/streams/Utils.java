/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka.streams;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.internals.StampedRecord;

import java.util.logging.Level;

public class Utils {

    private Utils() {}

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
            ConsumerRecord<?, ?> consumerRecord = (ConsumerRecord<?, ?>)record.value();
            if (consumerRecord != null) {
                acceptDistributedTraceHeader(consumerRecord);
            }

        } catch (ClassCastException ignored) {
            if (AgentBridge.getAgent().getLogger().isLoggable(Level.FINE)) {
                Logger logger = AgentBridge.getAgent().getLogger();
                logger.log(Level.FINE,
                        "Failed to get ConsumerRecord from StampedRecord in kafka streams transaction: {0}",
                        NewRelic.getAgent().getTransaction());
            }
        }

    }

    private static void acceptDistributedTraceHeader(ConsumerRecord<?, ?> consumerRecord) {
        ConsumerRecordWrapper wrapper = new ConsumerRecordWrapper(consumerRecord);
        NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Kafka, wrapper);
    }

    private static void nameTransaction(String ...parts) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "KafkaStreams", parts);
    }
}
