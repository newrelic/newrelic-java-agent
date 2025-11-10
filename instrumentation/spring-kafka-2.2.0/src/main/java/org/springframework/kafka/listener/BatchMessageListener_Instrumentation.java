/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.kafka.listener;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.kafka.SpringKafkaUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.support.Acknowledgment;

@Weave(originalName = "org.springframework.kafka.listener.BatchMessageListener", type = MatchType.Interface)
public class BatchMessageListener_Instrumentation<K,V> {

    @Trace(dispatcher = true)
    public void onMessage(ConsumerRecords<K, V> records, Acknowledgment acknowledgment, Consumer<K, V> consumer) {
        SpringKafkaUtil.processMessageListener(records);
        Weaver.callOriginal();
    }
}
