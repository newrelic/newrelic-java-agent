/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;

import static com.nr.instrumentation.kafka.Metrics.REBALANCE_ASSIGNED_BASE;
import static com.nr.instrumentation.kafka.Metrics.REBALANCE_REVOKED_BASE;

@Weave(type = MatchType.Interface, originalName = "org.apache.kafka.clients.consumer.ConsumerRebalanceListener")
public class ConsumerRebalanceListener_Instrumentation {

    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition topicPartition : partitions) {
            NewRelic.incrementCounter(REBALANCE_REVOKED_BASE + topicPartition.topic() + "/" + topicPartition.partition());
        }
        Weaver.callOriginal();
    }

    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition topicPartition : partitions) {
            NewRelic.incrementCounter(REBALANCE_ASSIGNED_BASE + topicPartition.topic() + "/" + topicPartition.partition());
        }
        Weaver.callOriginal();
    }

}
