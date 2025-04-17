/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring.kafka;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpringKafkaUtil {

    public static Map<Object, Boolean> listenerCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static String CATEGORY = "Message";
    public static String LIBRARY = "SpringKafka";

    private static final boolean DT_CONSUMER_ENABLED = NewRelic.getAgent().getConfig()
            .getValue("kafka.spans.distributed_trace.consume_many.enabled", false);

    public static <T> void processMessageListener(T data) {
        if (listenerCache.containsKey(data)) {
            return;
        }

        if (data instanceof ConsumerRecord) {
            List<?> records = Collections.singletonList(data);
            reportExternal(records, false);
        } else if (data instanceof ConsumerRecords) {
            reportExternal((ConsumerRecords<?, ?>) data, true);
        } else if (data instanceof List) {
            reportExternal((List<?>) data, true);
        }

        listenerCache.put(data, true);
    }

    public static void nameTransactionFromMethod(InvocableHandlerMethod handlerMethod) {
        if (AgentBridge.getAgent().getTransaction(false) != null && handlerMethod != null) {
            String fullMethodName = handlerMethod.getMethod().getDeclaringClass().getName() + "." + handlerMethod.getMethod().getName();
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true,
                    CATEGORY, LIBRARY, fullMethodName);

        }
    }

    private static void reportExternal(Iterable<?> consumerRecords, boolean isBatch) {
        boolean canAddHeaders = !isBatch || DT_CONSUMER_ENABLED;
        for (Object object: consumerRecords) {
            ConsumerRecord<?, ?> record = (object instanceof ConsumerRecord) ? (ConsumerRecord<?, ?>) object : null;
            if (record == null) {
                continue;
            }
            HeadersWrapper headers = canAddHeaders ? new HeadersWrapper(record.headers()) : null;

            MessageConsumeParameters params = MessageConsumeParameters.library(LIBRARY)
                    .destinationType(DestinationType.NAMED_TOPIC)
                    .destinationName(record.topic())
                    .inboundHeaders(headers)
                    .build();
            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
            break;
        }
    }

}
