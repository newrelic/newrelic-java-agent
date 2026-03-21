/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.springai;

import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import llm.events.LlmEvent;
import llm.models.ModelInvocation;
import llm.models.ModelRequest;
import llm.models.ModelResponse;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static llm.models.ModelInvocation.getRandomGuid;
import static llm.models.ModelInvocation.getTokenCount;
import static llm.models.ModelResponse.COMPLETION;
import static llm.models.ModelResponse.EMBEDDING;
import static llm.vendor.Vendor.SPRING_AI;

public class SpringAiModelInvocation implements ModelInvocation {
    Map<String, String> linkingMetadata;
    Map<String, Object> userAttributes;
    ModelRequest modelRequest;
    ModelResponse modelResponse;

    public SpringAiModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes,
            ChatClientRequest chatClientRequest, ChatClientResponse chatClientResponse) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new SpringAiModelRequest(chatClientRequest);
        this.modelResponse = new SpringAiModelResponse(chatClientResponse);
    }

    @Override
    public void setTracedMethodName(Transaction txn, String functionName) {
        txn.getTracedMethod().setMetricName("Llm", modelResponse.getOperationType(), SPRING_AI, functionName);
    }

    @Override
    public void setSegmentName(Segment segment, String functionName) {
        segment.setMetricName("Llm", modelResponse.getOperationType(), SPRING_AI, functionName);
    }

    @Override
    public void recordLlmEmbeddingEvent(long startTime, int index) {
        if (modelResponse.isErrorResponse()) {
            reportLlmError();
        }

        LlmEvent.Builder builder = new LlmEvent.Builder(this);

        LlmEvent llmEmbeddingEvent = builder // TODO
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(modelResponse.getLlmEmbeddingId())
                .requestId()
                .input(index)
                .requestModel()
                .responseModel()
                .responseOrganization()
                .responseUsageTotalTokens()
                .tokenCount(getTokenCount(modelRequest.getModelId(), modelRequest.getInputText(index)))
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
                .responseChoicesFinishReason()
                .responseOrganization()
                .responseUsagePromptTokens()
                .responseUsageCompletionTokens()
                .responseUsageTotalTokens()
                .timeToFirstToken(modelResponse.getTimeToFirstToken())
                .error()
                .duration(System.currentTimeMillis() - startTime)
                .build();

        llmChatCompletionSummaryEvent.recordLlmChatCompletionSummaryEvent();
    }

    @Override
    public void recordLlmChatCompletionMessageEvent(int sequence, String message, boolean isUser) {
        LlmEvent.Builder builder = new LlmEvent.Builder(this);

        LlmEvent llmChatCompletionMessageEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(getRandomGuid()) // FIXME is this right?
                .content(message)
                .role(modelResponse.isUser())
                .isResponse(modelResponse.isUser())
                .requestId()
                .responseModel()
                .sequence(sequence)
                .completionId()
                .tokenCount(getTokenCount(modelRequest.getModelId(), message))
                .build();

        llmChatCompletionMessageEvent.recordLlmChatCompletionMessageEvent();
    }

    @Override
    public void recordLlmEvents(long startTime) {
        String operationType = modelResponse.getOperationType();
        if (operationType.equals(COMPLETION)) {
            recordLlmChatCompletionEvents(startTime);
        } else if (operationType.equals(EMBEDDING)) {
            recordLlmEmbeddingEvents(startTime);
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
        int statusCode = modelResponse.getStatusCode();
        if (statusCode > 0) {
            errorParams.put("http.statusCode", statusCode); // not available from ChatClient
            errorParams.put("error.code", statusCode); // not available from ChatClient
        }
        if (!modelResponse.getLlmChatCompletionSummaryId().isEmpty()) {
            errorParams.put("completion_id", modelResponse.getLlmChatCompletionSummaryId());
        }
        if (!modelResponse.getLlmEmbeddingId().isEmpty()) {
            errorParams.put("embedding_id", modelResponse.getLlmEmbeddingId());
        }
        // FIXME statusText not available from ChatClient
        NewRelic.noticeError("LlmError: " + modelResponse.getStatusText(), errorParams);
    }

    /**
     * Records multiple LlmChatCompletionMessage events and a single LlmChatCompletionSummary event.
     * The number of LlmChatCompletionMessage events produced can differ based on vendor.
     */
    private void recordLlmChatCompletionEvents(long startTime) {
        int numberOfRequestMessages = modelRequest.getNumberOfRequestMessages();
        int numberOfResponseMessages = modelResponse.getNumberOfResponseMessages();
        int totalNumberOfMessages = numberOfRequestMessages + numberOfResponseMessages;

        int sequence = 0;

        // First, record all LlmChatCompletionMessage events representing the user input prompt
        for (int i = 0; i < numberOfRequestMessages; i++) {
            recordLlmChatCompletionMessageEvent(sequence, modelRequest.getRequestMessage(i), modelResponse.isUser());
            sequence++;
        }

        // Second, record all LlmChatCompletionMessage events representing the completion message from the LLM response
        for (int i = 0; i < numberOfResponseMessages; i++) {
            recordLlmChatCompletionMessageEvent(sequence, modelResponse.getResponseMessage(i), modelResponse.isUser());
            sequence++;
        }

        // Finally, record a summary event representing all LlmChatCompletionMessage events
        recordLlmChatCompletionSummaryEvent(startTime, totalNumberOfMessages);
    }

    /**
     * Records one, and potentially more, LlmEmbedding events based on the number of input messages in the request.
     * The number of LlmEmbedding events produced can differ based on vendor.
     */
    private void recordLlmEmbeddingEvents(long startTime) {
        int numberOfRequestMessages = modelRequest.getNumberOfInputTextMessages();
        // Record an LlmEmbedding event for each input message in the request
        for (int i = 0; i < numberOfRequestMessages; i++) {
            recordLlmEmbeddingEvent(startTime, i);
        }
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
