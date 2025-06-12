/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.azure.messaging.servicebus;

import com.azure.messaging.servicebus.implementation.MessagingEntityType;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.ServiceBusBatchRequestHeaders;
import com.newrelic.utils.ServiceBusUtil;
import reactor.core.publisher.Mono;

import java.util.logging.Level;

// Note: the ServiceBusSenderClient (non-Async) proxies to the Async client, so no need to instrument both
// that is not true for the receiver
@Weave(type = MatchType.ExactClass, originalName = "com.azure.messaging.servicebus.ServiceBusSenderAsyncClient")
public final class ServiceBusSenderAsyncClient_Instrumentation {

    private final MessagingEntityType entityType = Weaver.callOriginal();
    private final String entityName = Weaver.callOriginal();
    private final String fullyQualifiedNamespace = Weaver.callOriginal();
    @NewField
    public String nrEntityName = entityName;
    @NewField
    public String nrFullyQualifiedNamespace = fullyQualifiedNamespace;
    @NewField
    public MessagingEntityType nrEntityType = entityType;

    @WeaveAllConstructors
    ServiceBusSenderAsyncClient_Instrumentation() {
        // do nothing
    }

    // The iterable versions of sendMessages do not need to be instrumented,
    // as they end up calling the batch versions (below) anyway
//    public Mono<Void> sendMessages(Iterable<ServiceBusMessage> messages, ServiceBusTransactionContext transactionContext);
//    public Mono<Void> sendMessages(Iterable<ServiceBusMessage> messages);

    // this is called by the non-async version with the same signature
    @Trace
    public Mono<Void> sendMessage(ServiceBusMessage message) {
        ServiceBusBatchRequestHeaders headers = new ServiceBusBatchRequestHeaders(message);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        message = headers.tryToUpdateHeaders();

        MessageProduceParameters messageProduceParameters = ServiceBusUtil.generateExternalProduceMetrics(this);
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);

        return Weaver.callOriginal();
    }

    // this is called by the non-async version with the same signature
    @Trace
    public Mono<Void> sendMessage(ServiceBusMessage message, ServiceBusTransactionContext transactionContext) {
        ServiceBusBatchRequestHeaders headers = new ServiceBusBatchRequestHeaders(message);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        message = headers.tryToUpdateHeaders();

        MessageProduceParameters messageProduceParameters = ServiceBusUtil.generateExternalProduceMetrics(this);
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    // this is called by the non-async version with the same signature
    @Trace
    public Mono<Void> sendMessages(ServiceBusMessageBatch batch) {
        batch = tryToUpdateBatch(batch);

        MessageProduceParameters messageProduceParameters = ServiceBusUtil.generateExternalProduceMetrics(this);
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    // this is called by the non-async version with the same signature
    @Trace
    public Mono<Void> sendMessages(ServiceBusMessageBatch batch, ServiceBusTransactionContext transactionContext) {
        batch = tryToUpdateBatch(batch);

        MessageProduceParameters messageProduceParameters = ServiceBusUtil.generateExternalProduceMetrics(this);
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    // if we are unable to add DT headers for even 1 message here, we won't add it for any in the batch
    private ServiceBusMessageBatch tryToUpdateBatch(ServiceBusMessageBatch batch) {
        // first let's check to make sure the batch can hold all the new headers
        // Note: this won't be exact, but we dramatically overestimate the NR_DT_HEADER_SIZE to account
        if (batch.getMaxSizeInBytes() - batch.getSizeInBytes() < ServiceBusUtil.NR_DT_HEADER_SIZE * batch.getCount()) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "Unable to add DT headers to batch, not enough space in batch");
            return batch;
        }

        // now let's check to make sure that each message is small enough to add headers to
        // Note: this won't be exact, but we dramatically overestimate the NR_DT_HEADER_SIZE to account
        for (ServiceBusMessage message : batch.getMessages()) {
            if (message.getBody().getLength() + ServiceBusUtil.NR_DT_HEADER_SIZE > ServiceBusBatchRequestHeaders.DEFAULT_MAX_MESSAGE_SIZE) {
                NewRelic.getAgent().getLogger().log(Level.FINE, "Unable to add DT headers to batch, not enough space in message: "+message.getMessageId());
                return batch;
            }
        }

        for (ServiceBusMessage message : batch.getMessages()) {
            ServiceBusBatchRequestHeaders headers = new ServiceBusBatchRequestHeaders(message);
            NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
            // we've already checked for space, we can skip tryToUpdateHeaders(), it only adds the same check
            headers.addDTHeaders();
        }

        return batch;
    }
}
