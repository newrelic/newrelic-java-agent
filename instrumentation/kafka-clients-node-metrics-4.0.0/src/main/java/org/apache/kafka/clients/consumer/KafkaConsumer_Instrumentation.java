/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.apache.kafka.clients.consumer.KafkaConsumer")
public class KafkaConsumer_Instrumentation<K, V> {


    public void subscribe(SubscriptionPattern pattern) {
        Weaver.callOriginal();
    }

}
