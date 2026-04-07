/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.sqs;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.sqs.DefaultSqsAsyncClient")
class DefaultSqsAsyncClient_Instrumentation {

    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(SqsV2Util.LIBRARY, "sendMessage");

        if (SqsV2Util.canAddDtHeaders(sendMessageRequest)) {
            SQSRequestHeaders headers = new SQSRequestHeaders(sendMessageRequest);
            NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
            sendMessageRequest = headers.getUpdatedRequest();
        }

        segment.reportAsExternal(SqsV2Util.generateExternalProduceMetrics(sendMessageRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<SendMessageResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<SendMessageResponse, Throwable>() {
            @Override
            public void accept(SendMessageResponse sendMessageResponse, Throwable throwable) {
                SqsV2Util.finishSegment(segment);
            }
        });
    }

    public CompletableFuture<SendMessageBatchResponse> sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(SqsV2Util.LIBRARY, "sendMessageBatch");

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

        segment.reportAsExternal(SqsV2Util.generateExternalProduceMetrics(sendMessageBatchRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<SendMessageBatchResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<SendMessageBatchResponse, Throwable>() {
            @Override
            public void accept(SendMessageBatchResponse sendMessageBatchResponse, Throwable throwable) {
                SqsV2Util.finishSegment(segment);
            }
        });
    }

    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(SqsV2Util.LIBRARY, "receiveMessage");

        ArrayList<String> updatedMessageAttrNames = new ArrayList<>(receiveMessageRequest.messageAttributeNames());
        Collections.addAll(updatedMessageAttrNames, SqsV2Util.DT_HEADERS);
        receiveMessageRequest = receiveMessageRequest.toBuilder().messageAttributeNames(updatedMessageAttrNames).build();

        segment.reportAsExternal(SqsV2Util.generateExternalConsumeMetrics(receiveMessageRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ReceiveMessageResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<ReceiveMessageResponse, Throwable>() {
            @Override
            public void accept(ReceiveMessageResponse receiveMessageResponse, Throwable throwable) {
                SqsV2Util.finishSegment(segment);
            }
        });
    }
}
