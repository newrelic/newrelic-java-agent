/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka.spans;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Transaction;
import org.apache.kafka.clients.producer.ProducerRecord;

public class Utils {

    // Necessary to insert distributed traces here for debugging purposes
    public static void setDistributedTraceHeaders(ProducerRecord record) {
        final Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null && record != null) {
            transaction.insertDistributedTraceHeaders(new ProducerRecordWrapper(record));
        }}
}
