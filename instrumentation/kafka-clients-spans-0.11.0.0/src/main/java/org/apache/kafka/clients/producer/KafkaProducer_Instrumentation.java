/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.producer;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.kafka.spans.Utils;

import java.util.concurrent.Future;

@Weave(originalName = "org.apache.kafka.clients.producer.KafkaProducer")
public class KafkaProducer_Instrumentation<K, V> {

    @Trace
    private Future<RecordMetadata> doSend(ProducerRecord record, Callback callback) {
//        final Transaction transaction = AgentBridge.getAgent().getTransaction(false);
//        if (transaction != null) {
//            DistributedTracePayload payload = transaction.createDistributedTracePayload();
//            if (!(payload instanceof NoOpDistributedTracePayload)) {
//                record.headers().add("newrelic", payload.text().getBytes(StandardCharsets.UTF_8));
//            }
//        }
        Utils.setDistributedTraceHeaders(record);
        return Weaver.callOriginal();
    }
}
