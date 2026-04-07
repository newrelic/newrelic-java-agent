/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.utils.Timer;
import org.apache.kafka.common.Node;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.NewRelicMetricsReporter;

@Weave(originalName = "org.apache.kafka.clients.consumer.KafkaConsumer")
public class KafkaConsumer_Instrumentation<K, V> {

    public ConsumerRecords<K, V> poll(final long timeoutMs) {
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

    public ConsumerRecords<K, V> poll(final Duration timeout) {
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
        nrReportAsExternal(records);
        return records;
    }

    public void close() {
        try {
            Weaver.callOriginal();
        } catch (Exception e) {
            NewRelic.noticeError(e); // Record an error when a consumer fails to close (most likely due to a timeout)
            throw e;
        }
    }

    private void nrReportAsExternal(ConsumerRecords<K, V> records) {
        for (ConsumerRecord<K,V> record : records) {
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
    }

}
