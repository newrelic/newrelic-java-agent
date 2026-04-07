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
import com.newrelic.utils.SQSBatchRequestHeaders;
import com.newrelic.utils.SQSRequestHeaders;
import com.newrelic.utils.SqsV2Util;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.sqs.DefaultSqsClient")
class DefaultSqsClient_Instrumentation {

    @Trace
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        List<SendMessageBatchRequestEntry> processedEntries = new ArrayList<>();

        for (SendMessageBatchRequestEntry entry : sendMessageBatchRequest.entries()) {
            if (SqsV2Util.canAddDtHeaders(entry)) {
                SQSBatchRequestHeaders headers = new SQSBatchRequestHeaders(entry);
                NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
                processedEntries.add(headers.updatedEntry());
            } else {
                processedEntries.add(entry);
            }
        }
        sendMessageBatchRequest = sendMessageBatchRequest.toBuilder().entries(processedEntries).build();

        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics(sendMessageBatchRequest.queueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);

        return Weaver.callOriginal();
    }

    @Trace
    public SendMessageResponse sendMessage(SendMessageRequest sendMessageRequest) {
        if (SqsV2Util.canAddDtHeaders(sendMessageRequest)) {
            SQSRequestHeaders headers = new SQSRequestHeaders(sendMessageRequest);
            NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
            sendMessageRequest = headers.getUpdatedRequest();
        }

        MessageProduceParameters messageProduceParameters = SqsV2Util.generateExternalProduceMetrics(sendMessageRequest.queueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageProduceParameters);

        return Weaver.callOriginal();
    }

    @Trace
    public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        ArrayList<String> updatedMessageAttrNames = new ArrayList<>(receiveMessageRequest.messageAttributeNames());
        Collections.addAll(updatedMessageAttrNames, SqsV2Util.DT_HEADERS);
        receiveMessageRequest = receiveMessageRequest.toBuilder().messageAttributeNames(updatedMessageAttrNames).build();

        MessageConsumeParameters messageConsumeParameters = SqsV2Util.generateExternalConsumeMetrics(receiveMessageRequest.queueUrl());
        NewRelic.getAgent().getTracedMethod().reportAsExternal(messageConsumeParameters);

        return Weaver.callOriginal();
    }

}
