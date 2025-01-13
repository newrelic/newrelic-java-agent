/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.sqs;

import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.SqsV2Util;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.sqs.DefaultSqsClient")
class DefaultSqsClient_Instrumentation {

    @Trace
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics(sendMessageBatchRequest.queueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);

        return Weaver.callOriginal();
    }

    @Trace
    public SendMessageResponse sendMessage(SendMessageRequest sendMessageRequest) {
        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics(sendMessageRequest.queueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);

        return Weaver.callOriginal();
    }

    @Trace
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        MessageConsumeParameters messageConsumeParameters = SqsV2Util.generateExternalConsumeMetrics(receiveMessageRequest.queueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageConsumeParameters);

        return Weaver.callOriginal();
    }

}
