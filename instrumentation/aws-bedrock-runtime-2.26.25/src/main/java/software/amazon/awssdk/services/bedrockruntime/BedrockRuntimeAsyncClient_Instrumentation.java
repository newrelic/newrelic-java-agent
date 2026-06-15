/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.InternalServerException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelErrorException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceUnavailableException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service client for accessing Amazon Bedrock Runtime asynchronously.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient")
public abstract class BedrockRuntimeAsyncClient_Instrumentation {

    @Trace
    public CompletableFuture<ConverseResponse> converse(ConverseRequest converseRequest) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<ConverseResponse> converseResponseCompletableFuture = Weaver.callOriginal();

        return converseResponseCompletableFuture;
    }

    @Trace
    public CompletableFuture<Void> converseStream(ConverseStreamRequest converseStreamRequest, ConverseStreamResponseHandler asyncResponseHandler) {
        long startTime = System.currentTimeMillis();
        CompletableFuture<Void> voidCompletableFuture = Weaver.callOriginal();

        return voidCompletableFuture;
    }

//    @Trace
//    public ConverseResponse converse(Consumer<ConverseRequest.Builder> converseRequest) throws AccessDeniedException,
//            ResourceNotFoundException, ThrottlingException, ModelTimeoutException, InternalServerException,
//            ServiceUnavailableException, ValidationException, ModelNotReadyException, ModelErrorException, AwsServiceException,
//            SdkClientException, BedrockRuntimeException {
//        return converse(ConverseRequest.builder().applyMutation(converseRequest).build());
//    }

//    @Trace
//    public CompletableFuture<InvokeModelResponse> invokeModel(InvokeModelRequest invokeModelRequest) {
//        long startTime = System.currentTimeMillis();
//        CompletableFuture<InvokeModelResponse> invokeModelResponseFuture = Weaver.callOriginal();
//
//        if (isAiMonitoringEnabled()) {
//            Transaction txn = AgentBridge.getAgent().getTransaction();
//            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);
//
//            if (!(txn instanceof NoOpTransaction)) {
//                // Segment will be renamed later when the response is available
//                Segment segment = txn.startSegment("");
//                // Set llm = true agent attribute, this is required on transaction events
//                ModelInvocation.setLlmTrueAgentAttribute(txn);
//
//                // This should never happen, but protecting against bad implementations
//                if (invokeModelResponseFuture == null) {
//                    segment.end();
//                } else {
//                    Map<String, Object> userAttributes = txn.getUserAttributes();
//                    Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
//                    String modelId = invokeModelRequest.modelId();
//
//                    Token token = txn.getToken();
//
//                    // Instrumentation fails if the BiConsumer is replaced with a lambda
//                    invokeModelResponseFuture.whenComplete(new BiConsumer<InvokeModelResponse, Throwable>() {
//                        @Override
//                        public void accept(InvokeModelResponse invokeModelResponse, Throwable throwable) {
//
//                            try {
//                                if (modelId.toLowerCase().contains(ANTHROPIC_CLAUDE)) {
//                                    ModelInvocation claudeModelInvocation = new ClaudeModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
//                                            invokeModelResponse);
//                                    // Set segment name based on LLM operation from response
//                                    claudeModelInvocation.setSegmentName(segment, "invokeModel");
//                                    claudeModelInvocation.recordLlmEventsAsync(startTime, token);
//                                } else if (modelId.toLowerCase().contains(AMAZON_TITAN)) {
//                                    ModelInvocation titanModelInvocation = new TitanModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
//                                            invokeModelResponse);
//                                    // Set traced method name based on LLM operation from response
//                                    titanModelInvocation.setTracedMethodName(txn, "invokeModel");
//                                    titanModelInvocation.recordLlmEventsAsync(startTime, token);
//                                } else if (modelId.toLowerCase().contains(META_LLAMA_2)) {
//                                    ModelInvocation llama2ModelInvocation = new Llama2ModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
//                                            invokeModelResponse);
//                                    // Set traced method name based on LLM operation from response
//                                    llama2ModelInvocation.setTracedMethodName(txn, "invokeModel");
//                                    llama2ModelInvocation.recordLlmEventsAsync(startTime, token);
//                                } else if (modelId.toLowerCase().contains(COHERE_COMMAND) || modelId.toLowerCase().contains(COHERE_EMBED)) {
//                                    ModelInvocation commandModelInvocation = new CommandModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
//                                            invokeModelResponse);
//                                    // Set traced method name based on LLM operation from response
//                                    commandModelInvocation.setTracedMethodName(txn, "invokeModel");
//                                    commandModelInvocation.recordLlmEventsAsync(startTime, token);
//                                } else if (modelId.toLowerCase().contains(AI_21_LABS_JURASSIC)) {
//                                    ModelInvocation jurassicModelInvocation = new JurassicModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
//                                            invokeModelResponse);
//                                    // Set traced method name based on LLM operation from response
//                                    jurassicModelInvocation.setTracedMethodName(txn, "invokeModel");
//                                    jurassicModelInvocation.recordLlmEventsAsync(startTime, token);
//                                }
//                                if (segment != null) {
//                                    segment.endAsync();
//                                }
//                            } catch (Throwable t) {
//                                if (segment != null) {
//                                    segment.endAsync();
//                                }
//                                AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
//                            }
//                        }
//                    });
//                }
//            }
//        }
//        return invokeModelResponseFuture;
//    }
//
//    public CompletableFuture<Void> invokeModelWithResponseStream(
//            InvokeModelWithResponseStreamRequest invokeModelWithResponseStreamRequest,
//            InvokeModelWithResponseStreamResponseHandler asyncResponseHandler) {
//        if (isAiMonitoringEnabled()) {
//            if (isAiMonitoringStreamingEnabled()) {
//                NewRelic.getAgent()
//                        .getLogger()
//                        .log(Level.FINER,
//                                "aws-bedrock-runtime-2.20 instrumentation does not currently support response streaming. Enabling ai_monitoring.streaming will have no effect.");
//            }
//        }
//        return Weaver.callOriginal();
//    }
}
