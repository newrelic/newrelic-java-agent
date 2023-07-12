/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.runtime;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.connect.util.ConnectorTaskId;

import static com.nr.instrumentation.kafka.connect.KafkaConnectConstants.MESSAGE;
import static com.nr.instrumentation.kafka.connect.KafkaConnectConstants.KAFKA_CONNECT;

@Weave(originalName = "org.apache.kafka.connect.runtime.WorkerSinkTask")
abstract class WorkerSinkTask_Instrumentation {

    @Trace(dispatcher = true)
    protected void poll(long timeoutMs) {
        NewRelic.getAgent().getTransaction()
                .setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, MESSAGE, KAFKA_CONNECT, id().connector());
        Weaver.callOriginal();
    }

    private void convertMessages(ConsumerRecords<byte[], byte[]> msgs) {
        if (msgs.isEmpty()) {
            NewRelic.getAgent().getTransaction().ignore();
        }
        Weaver.callOriginal();
    }

    @Trace
    private void deliverMessages() {
        Weaver.callOriginal();
    }

    public abstract ConnectorTaskId id();
}