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

import static llm.models.SupportedModels.ANTHROPIC_CLAUDE;
import static llm.vendor.Vendor.VENDOR_VERSION;

/**
 * Service client for accessing Amazon Bedrock Runtime.
 */
@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient")
public abstract class BedrockRuntimeClient_Instrumentation {

    @Trace
    public InvokeModelResponse invokeModel(InvokeModelRequest invokeModelRequest) {
        long startTime = System.currentTimeMillis();
        InvokeModelResponse invokeModelResponse = Weaver.callOriginal();

        ModelInvocation.incrementInstrumentedSupportabilityMetric(VENDOR_VERSION);
        Transaction txn = AgentBridge.getAgent().getTransaction();

        // TODO check AIM config
        if (!(txn instanceof NoOpTransaction)) {
            Map<String, Object> userAttributes = txn.getUserAttributes();
            Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();

            String modelId = invokeModelRequest.modelId();
            if (modelId.toLowerCase().contains(ANTHROPIC_CLAUDE)) {
                ModelInvocation anthropicClaudeModelInvocation = new AnthropicClaudeModelInvocation(userAttributes, invokeModelRequest,
                        invokeModelResponse);
                // Set traced method name based on LLM operation
                anthropicClaudeModelInvocation.setTracedMethodName(txn, "invokeModel");
                // Set llm = true agent attribute
                ModelInvocation.setLlmTrueAgentAttribute(txn);
                anthropicClaudeModelInvocation.recordLlmEvents(startTime, linkingMetadata);
            }
        }

        return invokeModelResponse;
    }
}
