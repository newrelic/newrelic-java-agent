/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka.streams;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public class StreamsSpansUtil {
    private StreamsSpansUtil() {}

    public static void initTransaction() {
        LoopState.LOCAL.set(new LoopState());
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false,
                "Message", "Kafka/Streams/{Application Id}");
        // Todo: parse the application id from the thread name
    }

    // Records number of records poll to loop state
    public static void recordPolledToLoopState(ConsumerRecords<?, ?> records) {
        LoopState state = LoopState.LOCAL.get();
        if (state != null) {
            int polled = records == null ? 0 : records.count();
            state.incRecordsPolled(polled);
        }
    }

    public static void updateTotalProcessedToLoopState(double processed) {
        LoopState state = LoopState.LOCAL.get();
        if (state != null) {
            state.incTotalProcessed(processed);
        }

    }

    public static void endTransaction() {
        LoopState state = LoopState.LOCAL.get();
        if (state != null && state.getRecordsPolled() == 0 && state.getTotalProcessed() == 0) {
            NewRelic.getAgent().getTransaction().ignore();
        }
        LoopState.LOCAL.remove();
    }
}
