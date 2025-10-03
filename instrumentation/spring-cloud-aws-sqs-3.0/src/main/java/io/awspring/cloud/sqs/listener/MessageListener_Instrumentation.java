/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.awspring.cloud.sqs.listener;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.cloud.aws.sqs.SpringCloudAwsSqsUtil;
import org.springframework.messaging.Message;

@Weave(originalName = "io.awspring.cloud.sqs.listener.MessageListener", type = MatchType.Interface)
public class MessageListener_Instrumentation<T> {

    @Trace(dispatcher = true)
    public void onMessage(Message<T> message) {
        // Extract queue name from message headers if available
        String queueName = null;
        if (message != null && message.getHeaders().containsKey("Sqs_Queue")) {
            Object queueObj = message.getHeaders().get("Sqs_Queue");
            queueName = queueObj != null ? queueObj.toString() : null;
        }
        
        // Process message consumption
        SpringCloudAwsSqsUtil.processMessageConsumer(message, queueName);
        
        Weaver.callOriginal();
    }
}