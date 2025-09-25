/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.awspring.cloud.sqs.operations;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.spring.cloud.aws.sqs.SpringCloudAwsSqsUtil;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;

@Weave(originalName = "io.awspring.cloud.sqs.operations.SqsTemplate", type = MatchType.BaseClass)
public class SqsTemplate_Instrumentation {

    @Trace
    public SendMessageResponse send(SendMessageRequest request) {
        // Extract queue name and message attributes for tracing
        String queueUrl = request.queueUrl();
        String queueName = SpringCloudAwsSqsUtil.extractQueueNameFromUrl(queueUrl);
        Map<String, MessageAttributeValue> messageAttributes = request.messageAttributes();
        
        // Process message production
        SpringCloudAwsSqsUtil.processMessageProducer(queueName, messageAttributes);
        
        return Weaver.callOriginal();
    }

    @Trace
    public <T> SendMessageResponse send(String queueName, T message) {
        // Process message production for simple send
        SpringCloudAwsSqsUtil.processMessageProducer(queueName, null);
        
        return Weaver.callOriginal();
    }
}