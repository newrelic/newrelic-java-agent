/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.anthropic.claude;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import llm.events.LlmEvent;
import llm.models.ModelInvocation;
import llm.models.ModelRequest;
import llm.models.ModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static llm.models.anthropic.claude.AnthropicClaudeInvokeModelResponse.COMPLETION;
import static llm.models.anthropic.claude.AnthropicClaudeInvokeModelResponse.EMBEDDING;

public class AnthropicClaudeModelInvocation implements ModelInvocation {
    ModelRequest claudeRequest;
    ModelResponse claudeResponse;

    public AnthropicClaudeModelInvocation(InvokeModelRequest invokeModelRequest, InvokeModelResponse invokeModelResponse) {
        claudeRequest = new AnthropicClaudeInvokeModelRequest(invokeModelRequest);
        claudeResponse = new AnthropicClaudeInvokeModelResponse(invokeModelResponse);
    }

    @Override
    public void setLlmOperationMetricName(Transaction txn, String functionName) {
        txn.getTracedMethod().setMetricName("Llm", claudeResponse.getOperationType(), BEDROCK, functionName);
    }

    @Override
    public void recordLlmEmbeddingEvent(long startTime, Map<String, String> linkingMetadata) {
        if (claudeResponse.isErrorResponse()) {
            reportLlmError();
        }

        LlmEvent.Builder builder = new LlmEvent.Builder(linkingMetadata, claudeRequest, claudeResponse);

        LlmEvent llmEmbeddingEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(claudeResponse.getLlmEmbeddingId())
                .requestId()
                .input()
                .requestModel()
                .responseModel()
                .responseUsageTotalTokens()
                .responseUsagePromptTokens()
                .error()
                .duration(System.currentTimeMillis() - startTime)
                .build();

        llmEmbeddingEvent.recordLlmEmbeddingEvent();

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes? YES!
    }

    @Override
    public void recordLlmChatCompletionSummaryEvent(int numberOfMessages, long startTime, Map<String, String> linkingMetadata) {
        if (claudeResponse.isErrorResponse()) {
            reportLlmError();
        }

        LlmEvent.Builder builder = new LlmEvent.Builder(linkingMetadata, claudeRequest, claudeResponse);

        LlmEvent llmChatCompletionSummaryEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(claudeResponse.getLlmChatCompletionSummaryId())
                .requestId()
                .requestTemperature()
                .requestMaxTokens()
                .requestModel()
                .responseModel()
                .responseNumberOfMessages(numberOfMessages)
                .responseUsageTotalTokens()
                .responseUsagePromptTokens()
                .responseUsageCompletionTokens()
                .responseChoicesFinishReason()
                .error()
                .duration(System.currentTimeMillis() - startTime)
                .build();

        llmChatCompletionSummaryEvent.recordLlmChatCompletionSummaryEvent();

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes? YES!
    }

    @Override
    public void recordLlmChatCompletionMessageEvent(int sequence, String message, Map<String, String> linkingMetadata) {
        boolean isUser = message.contains("Human:");

        LlmEvent.Builder builder = new LlmEvent.Builder(linkingMetadata, claudeRequest, claudeResponse);

        LlmEvent llmChatCompletionMessageEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(ModelInvocation.getRandomGuid())
                .content(message)
                .role(isUser)
                .isResponse(isUser)
                .requestId()
                .responseModel()
                .sequence(sequence)
                .completionId()
                .build();

        llmChatCompletionMessageEvent.recordLlmChatCompletionMessageEvent();

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes? YES!
    }

    @Override
    public void recordLlmEvents(long startTime, Map<String, String> linkingMetadata) {
        String operationType = claudeResponse.getOperationType();
        if (operationType.equals(COMPLETION)) {
            recordLlmChatCompletionEvents(startTime, linkingMetadata);
        } else if (operationType.equals(EMBEDDING)) {
            recordLlmEmbeddingEvent(startTime, linkingMetadata);
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unexpected operation type encountered when trying to record LLM events");
        }
    }

    @Override
    public void reportLlmError() {
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("http.statusCode", claudeResponse.getStatusCode());
        errorParams.put("error.code", claudeResponse.getStatusCode());
        if (!claudeResponse.getLlmChatCompletionSummaryId().isEmpty()) {
            errorParams.put("completion_id", claudeResponse.getLlmChatCompletionSummaryId());
        }
        if (!claudeResponse.getLlmEmbeddingId().isEmpty()) {
            errorParams.put("embedding_id", claudeResponse.getLlmEmbeddingId());
        }
        NewRelic.noticeError("LlmError: " + claudeResponse.getStatusText(), errorParams);
    }

    /**
     * Records multiple LlmChatCompletionMessage events and a single LlmChatCompletionSummary event.
     * The number of LlmChatCompletionMessage events produced can differ based on vendor.
     */
    private void recordLlmChatCompletionEvents(long startTime, Map<String, String> linkingMetadata) {
        // First LlmChatCompletionMessage represents the user input prompt
        recordLlmChatCompletionMessageEvent(0, claudeRequest.getRequestMessage(), linkingMetadata);
        // Second LlmChatCompletionMessage represents the completion message from the LLM response
        recordLlmChatCompletionMessageEvent(1, claudeResponse.getResponseMessage(), linkingMetadata);
        // A summary of all LlmChatCompletionMessage events
        recordLlmChatCompletionSummaryEvent(2, startTime, linkingMetadata);
    }
}
