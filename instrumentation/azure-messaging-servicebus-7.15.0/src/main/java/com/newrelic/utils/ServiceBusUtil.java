/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient_Instrumentation;
import com.azure.messaging.servicebus.ServiceBusReceiverClient_Instrumentation;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient_Instrumentation;
import com.azure.messaging.servicebus.implementation.MessagingEntityType;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TransportType;
import reactor.core.publisher.Flux;

public class ServiceBusUtil {
    public static int NR_DT_HEADER_SIZE = 5000; // a large over-estimate, on purpose
    public static final String LIBRARY = "ServiceBus";
    public static final String OTEL_LIBRARY = null; // this is currently not instrumented by OTel

    public static MessageProduceParameters generateExternalProduceMetrics(ServiceBusSenderAsyncClient_Instrumentation client) {
        MessageProduceParameters params = MessageProduceParameters
                .library(LIBRARY, OTEL_LIBRARY)
                .destinationType(transalateMessageEntityTypeToDestinationType(client.nrEntityType))
                .destinationName(client.nrEntityName)
                .outboundHeaders(null)
                .instance(client.nrFullyQualifiedNamespace, null)
                .build();
        return params;
    }

    public static MessageConsumeParameters generateExternalConsumeMetrics(MessagingEntityType entityType, String namespace, String entityPath) {
        MessageConsumeParameters params = MessageConsumeParameters
                .library(LIBRARY, OTEL_LIBRARY)
                .destinationType(transalateMessageEntityTypeToDestinationType(entityType))
                .destinationName(entityPath)
                .inboundHeaders(null)
                .instance(namespace, null)
                .build();
        return params;
    }

    // this needs to happen here rather than in the weaved class, or a ClassNotFoundException will be thrown
    // also, we cannot register inner classes in the weaved classes for this package, as the JARs are signed
    public static Flux<ServiceBusReceivedMessage> registerFluxLifecycleHooks(Flux<ServiceBusReceivedMessage> result, Segment segment, Token token) {
        if (result == null) {
            return null;
        }

        return result
                .doOnNext(message -> {
                    token.linkAndExpire();
                    Headers headers = new HeadersWrapper(message.getApplicationProperties());
                    if (segment.getTransaction() != null) {
                        segment.getTransaction().acceptDistributedTraceHeaders(TransportType.ServiceBus, headers);
                    }
                })
                .doFinally(signalType -> {
                    segment.end();
                    token.linkAndExpire();
                });
    }

    public static ServiceBusReceivedMessage getFirstMessage(IterableStream<ServiceBusReceivedMessage> result) {
        return result.stream().peek(e -> {}).findFirst().orElse(null);
    }

    private static DestinationType transalateMessageEntityTypeToDestinationType(MessagingEntityType entityType) {
        if (entityType.equals(MessagingEntityType.TOPIC)) {
            return DestinationType.NAMED_TOPIC;
        }
        if (entityType.equals(MessagingEntityType.SUBSCRIPTION)) {
            return DestinationType.NAMED_TOPIC;
        }
        // otherwise just return Queue
        return DestinationType.NAMED_QUEUE;
    }

}
