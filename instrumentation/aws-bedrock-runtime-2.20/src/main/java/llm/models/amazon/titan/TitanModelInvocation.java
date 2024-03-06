/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.amazon.titan;

import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import llm.events.LlmEvent;
import llm.models.ModelInvocation;
import llm.models.ModelRequest;
import llm.models.ModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static llm.models.ModelResponse.COMPLETION;
import static llm.models.ModelResponse.EMBEDDING;
import static llm.vendor.Vendor.BEDROCK;

public class TitanModelInvocation implements ModelInvocation {
    Map<String, String> linkingMetadata;
    Map<String, Object> userAttributes;
    ModelRequest modelRequest;
    ModelResponse modelResponse;

    public TitanModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes, InvokeModelRequest invokeModelRequest,
            InvokeModelResponse invokeModelResponse) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new TitanModelRequest(invokeModelRequest);
        this.modelResponse = new TitanModelResponse(invokeModelResponse);
    }

    @Override
    public void setTracedMethodName(Transaction txn, String functionName) {
        txn.getTracedMethod().setMetricName("Llm", modelResponse.getOperationType(), BEDROCK, functionName);
    }

    @Override
    public void setSegmentName(Segment segment, String functionName) {
        segment.setMetricName("Llm", modelResponse.getOperationType(), BEDROCK, functionName);
    }

    @Override
    public void recordLlmEmbeddingEvent(long startTime) {
        if (modelResponse.isErrorResponse()) {
            reportLlmError();
        }

        LlmEvent.Builder builder = new LlmEvent.Builder(this);

        LlmEvent llmEmbeddingEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(modelResponse.getLlmEmbeddingId())
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
    }

    @Override
    public void recordLlmChatCompletionSummaryEvent(long startTime, int numberOfMessages) {
        if (modelResponse.isErrorResponse()) {
            reportLlmError();
        }

        LlmEvent.Builder builder = new LlmEvent.Builder(this);

        LlmEvent llmChatCompletionSummaryEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(modelResponse.getLlmChatCompletionSummaryId())
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
    }

    @Override
    public void recordLlmChatCompletionMessageEvent(int sequence, String message) {
        boolean isUser = sequence % 2 == 0;

        LlmEvent.Builder builder = new LlmEvent.Builder(this);

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
    }

    @Override
    public void recordLlmEvents(long startTime) {
        String operationType = modelResponse.getOperationType();
        if (operationType.equals(COMPLETION)) {
            recordLlmChatCompletionEvents(startTime);
        } else if (operationType.equals(EMBEDDING)) {
            recordLlmEmbeddingEvent(startTime);
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unexpected operation type encountered when trying to record LLM events");
        }
    }

    @Trace(async = true)
    @Override
    public void recordLlmEventsAsync(long startTime, Token token) {
        if (token != null && token.isActive()) {
            token.linkAndExpire();
        }
        recordLlmEvents(startTime);
    }

    @Override
    public void reportLlmError() {
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("http.statusCode", modelResponse.getStatusCode());
        errorParams.put("error.code", modelResponse.getStatusCode());
        if (!modelResponse.getLlmChatCompletionSummaryId().isEmpty()) {
            errorParams.put("completion_id", modelResponse.getLlmChatCompletionSummaryId());
        }
        if (!modelResponse.getLlmEmbeddingId().isEmpty()) {
            errorParams.put("embedding_id", modelResponse.getLlmEmbeddingId());
        }
        NewRelic.noticeError("LlmError: " + modelResponse.getStatusText(), errorParams);
    }

    /**
     * Records multiple LlmChatCompletionMessage events and a single LlmChatCompletionSummary event.
     * The number of LlmChatCompletionMessage events produced can differ based on vendor.
     */
    private void recordLlmChatCompletionEvents(long startTime) {
        // First LlmChatCompletionMessage represents the user input prompt
        recordLlmChatCompletionMessageEvent(0, modelRequest.getRequestMessage());
        // Second LlmChatCompletionMessage represents the completion message from the LLM response
        recordLlmChatCompletionMessageEvent(1, modelResponse.getResponseMessage());
        // A summary of all LlmChatCompletionMessage events
        recordLlmChatCompletionSummaryEvent(startTime, 2);
    }

    @Override
    public Map<String, String> getLinkingMetadata() {
        return linkingMetadata;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    @Override
    public ModelRequest getModelRequest() {
        return modelRequest;
    }

    @Override
    public ModelResponse getModelResponse() {
        return modelResponse;
    }
}
