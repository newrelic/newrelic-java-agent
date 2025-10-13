/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.producer;

import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.HeadersWrapper;

import java.util.concurrent.Future;

@Weave(originalName = "org.apache.kafka.clients.producer.KafkaProducer")
public class KafkaProducer_Instrumentation<K, V> {

    @Trace
    private Future<RecordMetadata> doSend(ProducerRecord record, Callback callback) {
        if (NewRelic.getAgent().getTransaction() != null) {
            Headers dtHeaders = new HeadersWrapper(record.headers());
            NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(dtHeaders);
        }
        return Weaver.callOriginal();
    }
}
