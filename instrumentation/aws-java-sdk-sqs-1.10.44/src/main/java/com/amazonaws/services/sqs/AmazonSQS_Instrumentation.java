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
import com.newrelic.utils.MetricUtil;

@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.sqs.AmazonSQS")
public class AmazonSQS_Instrumentation {

    @Trace
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        MessageProduceParameters messageProduceParameters = MetricUtil.generateExternalProduceMetrics(sendMessageBatchRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    @Trace
    public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
        MessageProduceParameters messageProduceParameters = MetricUtil.generateExternalProduceMetrics(sendMessageRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);
        return Weaver.callOriginal();
    }

    @Trace
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        MessageConsumeParameters messageConsumeParameters = MetricUtil.generateExternalConsumeMetrics(receiveMessageRequest.getQueueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageConsumeParameters);
        return Weaver.callOriginal();
    }
}
