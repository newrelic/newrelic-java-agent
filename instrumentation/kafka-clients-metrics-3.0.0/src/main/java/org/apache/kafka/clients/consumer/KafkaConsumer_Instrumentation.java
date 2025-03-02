/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nr.instrumentation.kafka.ClientType;
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

    private final Metrics metrics = Weaver.callOriginal();

    private final ConsumerMetadata metadata = Weaver.callOriginal();

    @NewField
    private boolean initialized;

    @WeaveAllConstructors
    public KafkaConsumer_Instrumentation() {
        if (!initialized) {
            List<Node> nodes = metadata.fetch().nodes();
            metrics.addReporter(new NewRelicMetricsReporter(ClientType.CONSUMER, nodes));
            initialized = true;
        }
    }

    private ConsumerRecords<K, V> poll(final Timer timer, final boolean includeMetadataInTimeout) {
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

    public void close() {
        try {
            Weaver.callOriginal();
        } catch (Exception e) {
            NewRelic.noticeError(e); // Record an error when a consumer fails to close (most likely due to a timeout)
            throw e;
        }
    }

}
