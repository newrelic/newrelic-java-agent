/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka.streams;

import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.streams.processor.internals.StampedRecord;

import java.nio.charset.StandardCharsets;

public class Utils {

    private Utils() {}

    public static void updateTransaction(StampedRecord record) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "KafkaStreams",
                "MessageBroker/Kafka/Topic/Consume/Named", record.topic(), "Streams/Task");

        ConcurrentHashMapHeaders dtHeaders = buildDtHeaders(record);
        NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Kafka, dtHeaders);

        MessageConsumeParameters consumeParams = MessageConsumeParameters.library("Kafka")
                .destinationType(DestinationType.NAMED_TOPIC)
                .destinationName(record.topic())
                .inboundHeaders(null)
                .build();
        NewRelic.getAgent().getTransaction().getTracedMethod().reportAsExternal(consumeParams);
    }

    private static ConcurrentHashMapHeaders buildDtHeaders(StampedRecord record) {
        ConcurrentHashMapHeaders dtHeaders = ConcurrentHashMapHeaders.build(HeaderType.MESSAGE);
        for (Header header: record.headers()) {
            if (header.value() != null) {
                dtHeaders.addHeader(header.key(), new String(header.value(), StandardCharsets.UTF_8));
            }
        }
        return dtHeaders;
    }

}
