/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring.cloud.aws.sqs;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

public class SpringCloudAwsSqsUtil {

    public static Map<Object, Boolean> listenerCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static String CATEGORY = "Message";
    public static String LIBRARY = "SpringCloudAwsSqs";

    /**
     * Process message consumption from @SqsListener methods
     */
    public static void processMessageConsumer(Message<?> message, String queueName) {
        if (message == null || listenerCache.containsKey(message)) {
            return;
        }

        // Extract message attributes for distributed tracing
        Object payload = message.getPayload();
        Map<String, MessageAttributeValue> messageAttributes = null;
        
        // Try to extract message attributes from the message headers
        if (message.getHeaders().containsKey("Sqs_MessageAttributes")) {
            Object attributesObj = message.getHeaders().get("Sqs_MessageAttributes");
            if (attributesObj instanceof Map) {
                messageAttributes = (Map<String, MessageAttributeValue>) attributesObj;
            }
        }

        SqsMessageHeaders headers = new SqsMessageHeaders(message, messageAttributes);

        MessageConsumeParameters params = MessageConsumeParameters.library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(queueName != null ? queueName : "Unknown")
                .inboundHeaders(headers)
                .build();

        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        listenerCache.put(message, true);
    }

    /**
     * Process message production through SqsTemplate
     */
    public static void processMessageProducer(String queueName, Map<String, MessageAttributeValue> messageAttributes) {
        if (queueName == null) {
            return;
        }

        SqsMessageHeaders headers = new SqsMessageHeaders(null, messageAttributes);

        MessageProduceParameters params = MessageProduceParameters.library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(queueName)
                .outboundHeaders(headers)
                .build();

        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
    }

    /**
     * Name transaction from @SqsListener handler method
     */
    public static void nameTransactionFromMethod(HandlerMethod handlerMethod) {
        if (AgentBridge.getAgent().getTransaction(false) != null && handlerMethod != null) {
            String fullMethodName = handlerMethod.getMethod().getDeclaringClass().getName() + "." + handlerMethod.getMethod().getName();
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true,
                    CATEGORY, LIBRARY, fullMethodName);
        }
    }

    /**
     * Extract queue name from queue URL
     */
    public static String extractQueueNameFromUrl(String queueUrl) {
        if (queueUrl == null) {
            return null;
        }
        
        // Extract queue name from SQS queue URL format: https://sqs.region.amazonaws.com/account/queue-name
        int lastSlash = queueUrl.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < queueUrl.length() - 1) {
            return queueUrl.substring(lastSlash + 1);
        }
        
        return queueUrl;
    }
}