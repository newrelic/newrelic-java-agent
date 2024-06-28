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
import com.newrelic.utils.MetricUtil;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.sqs.DefaultSqsAsyncClient")
class DefaultSqsAsyncClient_Instrumentation {

    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(MetricUtil.LIBRARY, "sendMessage");
        segment.reportAsExternal(MetricUtil.generateExternalProduceMetrics(sendMessageRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<SendMessageResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<SendMessageResponse, Throwable>() {
            @Override
            public void accept(SendMessageResponse sendMessageResponse, Throwable throwable) {
                MetricUtil.finishSegment(segment);
            }
        });
    }

    public CompletableFuture<SendMessageBatchResponse> sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(MetricUtil.LIBRARY, "sendMessageBatch");
        segment.reportAsExternal(MetricUtil.generateExternalProduceMetrics(sendMessageBatchRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<SendMessageBatchResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<SendMessageBatchResponse, Throwable>() {
            @Override
            public void accept(SendMessageBatchResponse sendMessageBatchResponse, Throwable throwable) {
                MetricUtil.finishSegment(segment);
            }
        });
    }

    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(MetricUtil.LIBRARY, "receiveMessage");
        segment.reportAsExternal(MetricUtil.generateExternalConsumeMetrics(receiveMessageRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ReceiveMessageResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<ReceiveMessageResponse, Throwable>() {
            @Override
            public void accept(ReceiveMessageResponse receiveMessageResponse, Throwable throwable) {
                MetricUtil.finishSegment(segment);
            }
        });
    }
}
