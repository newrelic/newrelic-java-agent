/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka.streams;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.streams.StreamsConfig;

public class StreamsSpansUtil {
    private StreamsSpansUtil() {}

    // Returns application id and if client.id is set, is added as a suffix seperated by /
    public static String getAppIdWithClientIdSuffix(StreamsConfig streamsConfig) {
        String applicationId = streamsConfig.getString(StreamsConfig.APPLICATION_ID_CONFIG);
        String clientId = streamsConfig.getString(StreamsConfig.CLIENT_ID_CONFIG);
        if (clientId == null || clientId.length() <= 0) {
            return applicationId;
        }
        return applicationId + "/" + clientId;
    }

    public static String getAppIdWithSuffix(String threadName) {
        final String defaultAppId = "APPLICATION_ID_UNKNOWN";
        String nrClientId = StreamsSpansUtil.parseClientId(threadName);
        if (nrClientId == null) {
            return defaultAppId;
        }
        // Gets the application id with a possible suffix using a global hashmap.
        return ClientIdToAppIdWithSuffixMap.getAppIdOrDefault(nrClientId, defaultAppId);

    }

    // Parses the client id out of the thread name. Could potentially cause a silent failure.
    private static String parseClientId(String threadName) {
        int idx = threadName.lastIndexOf("-StreamThread-");
        if (idx < 0) {
            return null;
        }
        return threadName.substring(0, idx);
    }

    public static void initTransaction(String applicationIdWithSuffix) {
        LoopState.LOCAL.set(new LoopState());
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false,
                "Message", "Kafka/Streams/" + applicationIdWithSuffix);
    }

    // Records number of records poll to loop state
    public static void recordPolledToLoopState(ConsumerRecords<?, ?> records) {
        LoopState state = LoopState.LOCAL.get();
        if (state != null) {
            int polled = records == null ? 0 : records.count();
            state.incRecordsPolled(polled);
        }
    }

    public static void incTotalProcessedToLoopState(double processed) {
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
