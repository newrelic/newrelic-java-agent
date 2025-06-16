/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.azure.messaging.servicebus;

import com.azure.messaging.servicebus.implementation.MessagingEntityType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.ServiceBusUtil;
import reactor.core.publisher.Flux;

@Weave(type = MatchType.ExactClass, originalName = "com.azure.messaging.servicebus.ServiceBusAsyncConsumer")
class ServiceBusAsyncConsumer_Instrumentation {

    @NewField
    public String nrEntityPath;
    @NewField
    public MessagingEntityType nrEntityType;
    @NewField
    public String nrNamespace;

    @Trace
    Flux<ServiceBusReceivedMessage> receive() {
        Transaction tx = NewRelic.getAgent().getTransaction();
        Token token = tx.getToken();
        Segment segment = tx.startSegment(ServiceBusUtil.LIBRARY, "receive");
        segment.reportAsExternal(ServiceBusUtil.generateExternalConsumeMetrics(nrEntityType, nrNamespace, nrEntityPath));

        Flux<ServiceBusReceivedMessage> result = Weaver.callOriginal();

        return ServiceBusUtil.registerFluxLifecycleHooks(result, segment, token);
    }
}
