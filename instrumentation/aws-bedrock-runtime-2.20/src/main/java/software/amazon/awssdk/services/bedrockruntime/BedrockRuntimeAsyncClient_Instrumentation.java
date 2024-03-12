/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

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
import llm.models.ai21labs.jurassic.JurassicModelInvocation;
import llm.models.amazon.titan.TitanModelInvocation;
import llm.models.anthropic.claude.ClaudeModelInvocation;
import llm.models.cohere.command.CommandModelInvocation;
import llm.models.meta.llama2.Llama2ModelInvocation;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamResponseHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringStreamingEnabled;
import static llm.models.SupportedModels.AI_21_LABS_JURASSIC;
import static llm.models.SupportedModels.AMAZON_TITAN;
import static llm.models.SupportedModels.ANTHROPIC_CLAUDE;
import static llm.models.SupportedModels.COHERE_COMMAND;
import static llm.models.SupportedModels.COHERE_EMBED;
import static llm.models.SupportedModels.META_LLAMA_2;
import static llm.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime asynchronously.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient")
public abstract class BedrockRuntimeAsyncClient_Instrumentation {

    @Trace
    public CompletableFuture<InvokeModelResponse> invokeModel(InvokeModelRequest invokeModelRequest) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<InvokeModelResponse> invokeModelResponseFuture = Weaver.callOriginal();

        if (isAiMonitoringEnabled()) {
            Transaction txn = AgentBridge.getAgent().getTransaction();
            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

            if (!(txn instanceof NoOpTransaction)) {
                // Segment will be renamed later when the response is available
                Segment segment = txn.startSegment("");
                // Set llm = true agent attribute, this is required on transaction events
                ModelInvocation.setLlmTrueAgentAttribute(txn);

                // This should never happen, but protecting against bad implementations
                if (invokeModelResponseFuture == null) {
                    segment.end();
                } else {
                    Map<String, Object> userAttributes = txn.getUserAttributes();
                    Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
                    String modelId = invokeModelRequest.modelId();

                    Token token = txn.getToken();

                    // Instrumentation fails if the BiConsumer is replaced with a lambda
                    invokeModelResponseFuture.whenComplete(new BiConsumer<InvokeModelResponse, Throwable>() {
                        @Override
                        public void accept(InvokeModelResponse invokeModelResponse, Throwable throwable) {

                            try {
                                if (modelId.toLowerCase().contains(ANTHROPIC_CLAUDE)) {
                                    ModelInvocation claudeModelInvocation = new ClaudeModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
                                            invokeModelResponse);
                                    // Set segment name based on LLM operation from response
                                    claudeModelInvocation.setSegmentName(segment, "invokeModel");
                                    claudeModelInvocation.recordLlmEventsAsync(startTime, token);
                                } else if (modelId.toLowerCase().contains(AMAZON_TITAN)) {
                                    ModelInvocation titanModelInvocation = new TitanModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
                                            invokeModelResponse);
                                    // Set traced method name based on LLM operation from response
                                    titanModelInvocation.setTracedMethodName(txn, "invokeModel");
                                    titanModelInvocation.recordLlmEventsAsync(startTime, token);
                                } else if (modelId.toLowerCase().contains(META_LLAMA_2)) {
                                    ModelInvocation llama2ModelInvocation = new Llama2ModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
                                            invokeModelResponse);
                                    // Set traced method name based on LLM operation from response
                                    llama2ModelInvocation.setTracedMethodName(txn, "invokeModel");
                                    llama2ModelInvocation.recordLlmEventsAsync(startTime, token);
                                } else if (modelId.toLowerCase().contains(COHERE_COMMAND) || modelId.toLowerCase().contains(COHERE_EMBED)) {
                                    ModelInvocation commandModelInvocation = new CommandModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
                                            invokeModelResponse);
                                    // Set traced method name based on LLM operation from response
                                    commandModelInvocation.setTracedMethodName(txn, "invokeModel");
                                    commandModelInvocation.recordLlmEventsAsync(startTime, token);
                                } else if (modelId.toLowerCase().contains(AI_21_LABS_JURASSIC)) {
                                    ModelInvocation jurassicModelInvocation = new JurassicModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
                                            invokeModelResponse);
                                    // Set traced method name based on LLM operation from response
                                    jurassicModelInvocation.setTracedMethodName(txn, "invokeModel");
                                    jurassicModelInvocation.recordLlmEventsAsync(startTime, token);
                                }
                                if (segment != null) {
                                    segment.endAsync();
                                }
                            } catch (Throwable t) {
                                if (segment != null) {
                                    segment.endAsync();
                                }
                                AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                            }
                        }
                    });
                }
            }
        }
        return invokeModelResponseFuture;
    }

    public CompletableFuture<Void> invokeModelWithResponseStream(
            InvokeModelWithResponseStreamRequest invokeModelWithResponseStreamRequest,
            InvokeModelWithResponseStreamResponseHandler asyncResponseHandler) {
        if (isAiMonitoringEnabled()) {
            if (isAiMonitoringStreamingEnabled()) {
                NewRelic.getAgent()
                        .getLogger()
                        .log(Level.FINER,
                                "aws-bedrock-runtime-2.20 instrumentation does not currently support response streaming. Enabling ai_monitoring.streaming will have no effect.");
            }
        }
        return Weaver.callOriginal();
    }
}
