/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.azure.messaging.servicebus;

import com.azure.core.util.IterableStream;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.HeadersWrapper;
import com.newrelic.utils.ServiceBusUtil;

import java.time.Duration;

@Weave(type = MatchType.ExactClass, originalName = "com.azure.messaging.servicebus.ServiceBusReceiverClient")
public class ServiceBusReceiverClient_Instrumentation {

    private final ServiceBusReceiverAsyncClient asyncClient = Weaver.callOriginal();
    @NewField
    public ServiceBusReceiverAsyncClient_Instrumentation nrAsyncClient =
            ServiceBusReceiverAsyncClient_Instrumentation.class.cast(asyncClient);

    @WeaveAllConstructors
    ServiceBusReceiverClient_Instrumentation() {
        // do nothing
    }

    // this method just calls the one with maxWaitTime, no need to instrument both
    //public IterableStream<ServiceBusReceivedMessage> receiveMessages(int maxMessages);

    @Trace
    public IterableStream<ServiceBusReceivedMessage> receiveMessages(int maxMessages, Duration maxWaitTime) {
        NewRelic.getAgent().getTracedMethod().reportAsExternal(ServiceBusUtil.generateExternalConsumeMetrics(
                nrAsyncClient.nrEntityType, nrAsyncClient.getFullyQualifiedNamespace(), nrAsyncClient.getEntityPath()));

        IterableStream<ServiceBusReceivedMessage> result = Weaver.callOriginal();

        ServiceBusReceivedMessage firstMessage = ServiceBusUtil.getFirstMessage(result);  // can't be done inside the weaved class
        if (firstMessage != null) {
            Headers headers = new HeadersWrapper(firstMessage.getApplicationProperties());
            NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.ServiceBus, headers);
        }

        return result;
    }
}
