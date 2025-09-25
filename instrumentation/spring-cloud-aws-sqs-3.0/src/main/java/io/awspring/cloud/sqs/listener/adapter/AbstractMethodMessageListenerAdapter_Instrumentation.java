/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.awspring.cloud.sqs.listener.adapter;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.cloud.aws.sqs.SpringCloudAwsSqsUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;

@Weave(originalName = "io.awspring.cloud.sqs.listener.adapter.AbstractMethodMessageListenerAdapter", type = MatchType.BaseClass)
public class AbstractMethodMessageListenerAdapter_Instrumentation {

    @Trace(dispatcher = true)
    protected Object doInvokeMethod(HandlerMethod handlerMethod, Message<?> message, Object... additionalArgs) {
        // Set transaction name based on handler method
        SpringCloudAwsSqsUtil.nameTransactionFromMethod(handlerMethod);
        
        // Extract queue name from message headers if available
        String queueName = null;
        if (message != null && message.getHeaders().containsKey("Sqs_Queue")) {
            Object queueObj = message.getHeaders().get("Sqs_Queue");
            queueName = queueObj != null ? queueObj.toString() : null;
        }
        
        // Process message consumption
        SpringCloudAwsSqsUtil.processMessageConsumer(message, queueName);
        
        return Weaver.callOriginal();
    }
}