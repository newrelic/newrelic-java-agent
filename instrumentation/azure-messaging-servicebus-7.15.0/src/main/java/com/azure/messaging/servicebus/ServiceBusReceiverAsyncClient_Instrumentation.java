/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.azure.messaging.servicebus;

import com.azure.messaging.servicebus.implementation.MessagingEntityType;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient")
public class ServiceBusReceiverAsyncClient_Instrumentation {

    private final MessagingEntityType entityType = Weaver.callOriginal();
    @NewField
    public MessagingEntityType nrEntityType = entityType;

    @WeaveAllConstructors
    ServiceBusReceiverAsyncClient_Instrumentation() {
        // do nothing
    }

    private ServiceBusAsyncConsumer_Instrumentation getOrCreateConsumer() {
        ServiceBusAsyncConsumer_Instrumentation result = Weaver.callOriginal();
        result.nrEntityPath = getEntityPath();
        result.nrEntityType = entityType;
        result.nrNamespace = getFullyQualifiedNamespace();

        return result;
    }

    public String getFullyQualifiedNamespace() {
        return Weaver.callOriginal();
    }

    public String getEntityPath() {
        return Weaver.callOriginal();
    }

}
