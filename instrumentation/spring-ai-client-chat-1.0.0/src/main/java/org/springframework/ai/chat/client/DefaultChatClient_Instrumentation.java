/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.chat.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import llm.completions.models.ModelInvocation;
import llm.completions.models.springai.SpringAiModelInvocation;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static llm.completions.vendor.Vendor.VENDOR_VERSION;

public class DefaultChatClient_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "org.springframework.ai.chat.client.DefaultChatClient$DefaultCallResponseSpec")
    public static class DefaultCallResponseSpec_Instrumentation {

        /**
         * Spring ChatClient call() gets processed here. These are synchronous.
         */
        @Trace
        private ChatClientResponse doGetObservableChatClientResponse(ChatClientRequest chatClientRequest) {
            long startTime = System.currentTimeMillis();

            ChatClientResponse chatClientResponse = Weaver.callOriginal();

            if (isAiMonitoringEnabled()) {
                Transaction txn = AgentBridge.getAgent().getTransaction();
                ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

                if (!(txn instanceof NoOpTransaction)) {
                    // Set llm = true agent attribute, this is required on transaction events
                    ModelInvocation.setLlmTrueAgentAttribute(txn);

                    Map<String, Object> userAttributes = txn.getUserAttributes();
                    Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();

                    // Create Spring AI model invocation
                    ModelInvocation springAiInvocation = new SpringAiModelInvocation(
                            linkingMetadata, userAttributes, chatClientRequest, chatClientResponse);
                    springAiInvocation.setTracedMethodName(txn, "call");
                    springAiInvocation.recordLlmEvents(startTime);
                }
            }

            return chatClientResponse;
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "org.springframework.ai.chat.client.DefaultChatClient$DefaultStreamResponseSpec")
    public static class DefaultStreamResponseSpec_Instrumentation {

        /**
         * Spring ChatClient stream() gets processed here.
         */
        @Trace
        private Flux<ChatClientResponse> doGetObservableFluxChatResponse(ChatClientRequest chatClientRequest) {
            long startTime = System.currentTimeMillis();
            Flux<ChatClientResponse> chatClientResponseFlux = Weaver.callOriginal();

            if (isAiMonitoringEnabled()) {
                Transaction txn = AgentBridge.getAgent().getTransaction();
                ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

                if (!(txn instanceof NoOpTransaction)) {
                    // Segment will be renamed later when the response is available
                    Segment segment = txn.startSegment("");
                    // Set llm = true agent attribute, this is required on transaction events
                    ModelInvocation.setLlmTrueAgentAttribute(txn);

                    // This should never happen, but protecting against bad implementations
                    if (chatClientResponseFlux == null) {
                        segment.end();
                    } else {
                        Map<String, Object> userAttributes = txn.getUserAttributes();
                        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();

                        try {
                            // collect all chunks of the stream response into a list without blocking
                            // must use anonymous classes instead of lambdas or else instrumentation won't apply
                            chatClientResponseFlux.collectList().subscribe(
                                    // onNext consumer
                                    new Consumer<List<ChatClientResponse>>() {
                                        @Override
                                        public void accept(List<ChatClientResponse> chatClientResponseList) {
                                            List<ChatClientResponse> list = chatClientResponseList != null ? chatClientResponseList : new ArrayList<>();

                                            // Create Spring AI model invocation
                                            ModelInvocation springAiInvocation = new SpringAiModelInvocation(
                                                    linkingMetadata, userAttributes, chatClientRequest, list);
                                            springAiInvocation.setSegmentName(segment, "stream");
                                            springAiInvocation.recordLlmEventsAsync(startTime, null);
                                        }
                                    },
                                    // onError consumer
                                    new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable error) {
                                            if (segment != null) {
                                                segment.endAsync();
                                            }
                                        }
                                    },
                                    // onComplete consumer
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (segment != null) {
                                                segment.endAsync();
                                            }
                                        }
                                    }
                            );
                        } catch (Throwable t) {
                            if (segment != null) {
                                segment.endAsync();
                            }
                            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                        }
                    }
                }
            }
            return chatClientResponseFlux;
        }
    }
}
