/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;

// we are only weaving this method because it was introduced in kafka-clients 2.0.0 and it's easier than copying a
// whole instrumentation module
@Weave(originalName = "org.apache.kafka.clients.consumer.KafkaConsumer")
public class KafkaConsumer_Instrumentation<K, V> {

    public ConsumerRecords<K, V> poll(Duration timeout) {
        final ConsumerRecords<K, V> records;
        try {
            records = Weaver.callOriginal();
        } catch (Exception e) {
            // Specifically ignore WakeupExceptions because they are common in non-error use cases
            if (!(e instanceof WakeupException)) {
                NewRelic.noticeError(e);
            }
            throw e;
        }

        for (ConsumerRecord record : records) {
            if (AgentBridge.getAgent().getTransaction(false) != null) {
                MessageConsumeParameters params = MessageConsumeParameters.library("Kafka")
                        .destinationType(DestinationType.NAMED_TOPIC)
                        .destinationName(record.topic())
                        .inboundHeaders(null)
                        .build();
                NewRelic.getAgent().getTransaction().getTracedMethod().reportAsExternal(params);
            }
            break;
        }
        return records;
    }
}
