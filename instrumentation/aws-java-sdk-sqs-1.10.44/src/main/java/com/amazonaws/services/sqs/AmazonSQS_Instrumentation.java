/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.sqs;

import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.SQSBatchRequestHeaders;
import com.newrelic.utils.SQSRequestHeaders;
import com.newrelic.utils.SqsV1Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.sqs.AmazonSQS")
public class AmazonSQS_Instrumentation {

    @Trace
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        for (SendMessageBatchRequestEntry request : sendMessageBatchRequest.getEntries()) {
            SQSBatchRequestHeaders headers = new SQSBatchRequestHeaders(request);
            NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        }

        MessageProduceParameters messageProduceParameters = SqsV1Util.generateExternalProduceMetrics(sendMessageBatchRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    @Trace
    public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
        SQSRequestHeaders headers = new SQSRequestHeaders(sendMessageRequest);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);

        MessageProduceParameters messageProduceParameters = SqsV1Util.generateExternalProduceMetrics(sendMessageRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    @Trace
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        List<String> updatedMessageAttrNames = new ArrayList<>(receiveMessageRequest.getMessageAttributeNames());
        Collections.addAll(updatedMessageAttrNames, SqsV1Util.DT_HEADERS);
        receiveMessageRequest.setMessageAttributeNames(updatedMessageAttrNames);

        MessageConsumeParameters messageConsumeParameters = SqsV1Util.generateExternalConsumeMetrics(receiveMessageRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageConsumeParameters);
        return Weaver.callOriginal();
    }

}
