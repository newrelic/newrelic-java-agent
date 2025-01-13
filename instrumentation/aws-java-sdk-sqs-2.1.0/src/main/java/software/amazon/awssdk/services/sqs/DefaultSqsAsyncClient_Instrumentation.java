/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.sqs;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.SqsV2Util;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.sqs.DefaultSqsAsyncClient")
class DefaultSqsAsyncClient_Instrumentation {

    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(SqsV2Util.LIBRARY, "sendMessage");

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
