/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.chat.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import llm.models.ModelInvocation;
import llm.models.springai.SpringAiModelInvocation;
import reactor.core.publisher.Flux;

import java.util.Map;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static llm.vendor.Vendor.VENDOR_VERSION;

//@Weave(type = MatchType.ExactClass, originalName = "org.springframework.ai.chat.client.DefaultChatClient")
public class DefaultChatClient_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "org.springframework.ai.chat.client.DefaultChatClient$DefaultCallResponseSpec")
    public static class DefaultCallResponseSpec_Instrumentation {

        /**
         * Spring ChatClient call() gets processed here
         */
        @Trace
        private ChatClientResponse doGetObservableChatClientResponse(ChatClientRequest chatClientRequest) {
            long startTime = System.currentTimeMillis(); // FIXME start timing here? or in call/stream?

            ChatClientResponse chatClientResponse = Weaver.callOriginal();

            if (isAiMonitoringEnabled()) {
                Transaction txn = AgentBridge.getAgent().getTransaction();
                ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION); // FIXME doesn't need to be called repeatedly

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
         * Spring ChatClient stream() gets processed here
         */
        @Trace
        private Flux<ChatClientResponse> doGetObservableFluxChatResponse(ChatClientRequest chatClientRequest) {
            long startTime = System.currentTimeMillis(); // FIXME start timing here? or in call/stream?
            Flux<ChatClientResponse> chatClientResponseFlux = Weaver.callOriginal();

            if (isAiMonitoringEnabled()) {
                Transaction txn = AgentBridge.getAgent().getTransaction();
                ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION); // FIXME doesn't need to be called repeatedly?

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

                        Token token = txn.getToken();
                        try {
                            // TODO debug and clean up
                            chatClientResponseFlux.doOnEach(signal -> {
                                if (signal.isOnError()) {
                                    if (segment != null) {
                                        segment.endAsync();
                                    }
                                    if (token != null) {
                                        token.expire();
                                    }
                                } else if (signal.isOnNext()) {
                                    // Create Spring AI model invocation
                                    ModelInvocation springAiInvocation = new SpringAiModelInvocation(
                                            linkingMetadata, userAttributes, chatClientRequest, signal.get());
                                    springAiInvocation.setSegmentName(segment, "stream");
                                    springAiInvocation.recordLlmEventsAsync(startTime, token);
                                }
                            }).subscribe();
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
