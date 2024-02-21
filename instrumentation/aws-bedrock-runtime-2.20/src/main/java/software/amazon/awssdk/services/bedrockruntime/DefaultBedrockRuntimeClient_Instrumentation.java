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
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Map;
import java.util.logging.Level;

import static llm.models.ModelInvocation.ANTHROPIC_CLAUDE;
import static llm.models.anthropic.claude.AnthropicClaudeInvokeModelResponse.COMPLETION;
import static llm.models.anthropic.claude.AnthropicClaudeInvokeModelResponse.EMBEDDING;

/**
 * Service client for accessing Amazon Bedrock Runtime.
 */
// TODO switch back to instrumenting the BedrockRuntimeClient interface instead of this implementation class
@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.bedrockruntime.DefaultBedrockRuntimeClient")
final class DefaultBedrockRuntimeClient_Instrumentation {
//    private static final Logger log = Logger.loggerFor(DefaultBedrockRuntimeClient.class);
//
//    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
//            .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final BedrockRuntimeServiceClientConfiguration serviceClientConfiguration;

    protected DefaultBedrockRuntimeClient_Instrumentation(BedrockRuntimeServiceClientConfiguration serviceClientConfiguration,
            SdkClientConfiguration clientConfiguration) {
        this.clientHandler = Weaver.callOriginal();
        this.clientConfiguration = Weaver.callOriginal();
        this.serviceClientConfiguration = Weaver.callOriginal();
        this.protocolFactory = Weaver.callOriginal();
    }

    @Trace
    public InvokeModelResponse invokeModel(InvokeModelRequest invokeModelRequest) {
        long startTime = System.currentTimeMillis();
        InvokeModelResponse invokeModelResponse = Weaver.callOriginal();

        ModelInvocation.incrementInstrumentedSupportabilityMetric();

//        Transaction txn = NewRelic.getAgent().getTransaction();
        Transaction txn = AgentBridge.getAgent().getTransaction();
        // TODO check AIM config
        if (txn != null && !(txn instanceof NoOpTransaction)) {
            Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
            Map<String, Object> userAttributes = txn.getUserAttributes();

            String modelId = invokeModelRequest.modelId();
            if (modelId.toLowerCase().contains(ANTHROPIC_CLAUDE)) {
                AnthropicClaudeModelInvocation anthropicClaudeModelInvocation = new AnthropicClaudeModelInvocation(invokeModelRequest,
                        invokeModelResponse);
                // Set traced method name based on LLM operation
                anthropicClaudeModelInvocation.setLlmOperationMetricName(txn, "invokeModel");
                // Set llm = true agent attribute
                ModelInvocation.setLlmTrueAgentAttribute(txn);
                anthropicClaudeModelInvocation.recordLlmEvents(startTime, linkingMetadata);
            }
        }

        return invokeModelResponse;
    }

}
