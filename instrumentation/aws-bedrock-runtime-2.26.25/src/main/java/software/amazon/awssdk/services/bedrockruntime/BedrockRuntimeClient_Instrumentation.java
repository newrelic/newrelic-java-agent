/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import llm.models.ModelInvocation;
import llm.models.converse.ConverseModelInvocation;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InternalServerException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelErrorException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceUnavailableException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import java.util.Map;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static llm.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime synchronously.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient")
public abstract class BedrockRuntimeClient_Instrumentation {

    @Trace
    public ConverseResponse converse(ConverseRequest converseRequest) throws AccessDeniedException, ResourceNotFoundException,
            ThrottlingException, ModelTimeoutException, InternalServerException, ServiceUnavailableException,
            ValidationException, ModelNotReadyException, ModelErrorException, AwsServiceException, SdkClientException,
            BedrockRuntimeException {

        long startTime = System.currentTimeMillis();
        ConverseResponse converseResponse = Weaver.callOriginal();

        if (isAiMonitoringEnabled()) {
            Transaction txn = AgentBridge.getAgent().getTransaction();
            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

            if (!(txn instanceof NoOpTransaction)) {
                // Set llm = true agent attribute, this is required on transaction events
                ModelInvocation.setLlmTrueAgentAttribute(txn);

                Map<String, Object> userAttributes = txn.getUserAttributes();
                Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
//                String modelId = invokeModelRequest.modelId();

                ModelInvocation converseModelInvocation = new ConverseModelInvocation(linkingMetadata, userAttributes, converseRequest, converseResponse);

                // Set traced method name based on LLM operation from response
                converseModelInvocation.setTracedMethodName(txn, "converse");
                converseModelInvocation.recordLlmEvents(startTime);

            }
        }

        return converseResponse;
    }

//    @Trace
//    public InvokeModelResponse invokeModel(InvokeModelRequest invokeModelRequest) {
//        long startTime = System.currentTimeMillis();
//        InvokeModelResponse invokeModelResponse = Weaver.callOriginal();
//
//        if (isAiMonitoringEnabled()) {
//            Transaction txn = AgentBridge.getAgent().getTransaction();
//            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);
//
//            if (!(txn instanceof NoOpTransaction)) {
//                // Set llm = true agent attribute, this is required on transaction events
//                ModelInvocation.setLlmTrueAgentAttribute(txn);
//
//                Map<String, Object> userAttributes = txn.getUserAttributes();
//                Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
//                String modelId = invokeModelRequest.modelId();
//
//                if (modelId.toLowerCase().contains(ANTHROPIC_CLAUDE)) {
//                    ModelInvocation claudeModelInvocation = new ClaudeModelInvocation(linkingMetadata, userAttributes, invokeModelRequest, invokeModelResponse);
//                    // Set traced method name based on LLM operation from response
//                    claudeModelInvocation.setTracedMethodName(txn, "invokeModel");
//                    claudeModelInvocation.recordLlmEvents(startTime);
//                } else if (modelId.toLowerCase().contains(AMAZON_TITAN)) {
//                    ModelInvocation titanModelInvocation = new TitanModelInvocation(linkingMetadata, userAttributes, invokeModelRequest, invokeModelResponse);
//                    // Set traced method name based on LLM operation from response
//                    titanModelInvocation.setTracedMethodName(txn, "invokeModel");
//                    titanModelInvocation.recordLlmEvents(startTime);
//                } else if (modelId.toLowerCase().contains(META_LLAMA_2)) {
//                    ModelInvocation llama2ModelInvocation = new Llama2ModelInvocation(linkingMetadata, userAttributes, invokeModelRequest, invokeModelResponse);
//                    // Set traced method name based on LLM operation from response
//                    llama2ModelInvocation.setTracedMethodName(txn, "invokeModel");
//                    llama2ModelInvocation.recordLlmEvents(startTime);
//                } else if (modelId.toLowerCase().contains(COHERE_COMMAND) || modelId.toLowerCase().contains(COHERE_EMBED)) {
//                    ModelInvocation commandModelInvocation = new CommandModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
//                            invokeModelResponse);
//                    // Set traced method name based on LLM operation from response
//                    commandModelInvocation.setTracedMethodName(txn, "invokeModel");
//                    commandModelInvocation.recordLlmEvents(startTime);
//                } else if (modelId.toLowerCase().contains(AI_21_LABS_JURASSIC)) {
//                    ModelInvocation jurassicModelInvocation = new JurassicModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
//                            invokeModelResponse);
//                    // Set traced method name based on LLM operation from response
//                    jurassicModelInvocation.setTracedMethodName(txn, "invokeModel");
//                    jurassicModelInvocation.recordLlmEvents(startTime);
//                }
//            }
//        }
//        return invokeModelResponse;
//    }
}
