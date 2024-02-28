/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
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
import llm.models.anthropic.claude.AnthropicClaudeModelInvocation;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Map;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.isAiMonitoringEnabled;
import static llm.models.SupportedModels.AI_21_LABS_JURASSIC;
import static llm.models.SupportedModels.AMAZON_TITAN;
import static llm.models.SupportedModels.ANTHROPIC_CLAUDE;
import static llm.models.SupportedModels.COHERE_COMMAND;
import static llm.models.SupportedModels.META_LLAMA_2;
import static llm.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime synchronously.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient")
public abstract class BedrockRuntimeClient_Instrumentation {

    @Trace
    public InvokeModelResponse invokeModel(InvokeModelRequest invokeModelRequest) {
        long startTime = System.currentTimeMillis();
        InvokeModelResponse invokeModelResponse = Weaver.callOriginal();

        if (isAiMonitoringEnabled()) {
            Transaction txn = AgentBridge.getAgent().getTransaction();
            ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);

            if (!(txn instanceof NoOpTransaction)) {
                // Set llm = true agent attribute, this is required on transaction events
                ModelInvocation.setLlmTrueAgentAttribute(txn);

                Map<String, Object> userAttributes = txn.getUserAttributes();
                Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
                String modelId = invokeModelRequest.modelId();

                if (modelId.toLowerCase().contains(ANTHROPIC_CLAUDE)) {
                    ModelInvocation anthropicClaudeModelInvocation = new AnthropicClaudeModelInvocation(linkingMetadata, userAttributes, invokeModelRequest,
                            invokeModelResponse);
                    // Set traced method name based on LLM operation from response
                    anthropicClaudeModelInvocation.setTracedMethodName(txn, "invokeModel");
                    anthropicClaudeModelInvocation.recordLlmEvents(startTime);
                } else if (modelId.toLowerCase().contains(AMAZON_TITAN)) {

                } else if (modelId.toLowerCase().contains(META_LLAMA_2)) {

                } else if (modelId.toLowerCase().contains(COHERE_COMMAND)) {

                } else if (modelId.toLowerCase().contains(AI_21_LABS_JURASSIC)) {

                }
            }
        }
        return invokeModelResponse;
    }
}
