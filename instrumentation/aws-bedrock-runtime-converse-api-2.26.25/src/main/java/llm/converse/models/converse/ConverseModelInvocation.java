/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.converse.models.converse;

import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountResolver;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import llm.converse.events.LlmEvent;
import llm.converse.models.ModelInvocation;
import llm.converse.models.ModelRequest;
import llm.converse.models.ModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static llm.converse.models.ModelInvocation.getRandomGuid;
import static llm.converse.models.ModelResponse.COMPLETION;
import static llm.converse.vendor.Vendor.BEDROCK;

public class ConverseModelInvocation implements ModelInvocation {
    Map<String, String> linkingMetadata;
    Map<String, Object> userAttributes;
    ModelRequest modelRequest;
    ModelResponse modelResponse;
    int timeToFirstToken;

    /**
     * Construct a ConverseModelInvocation representing the
     * request and response for a non-streaming API.
     *
     * @param linkingMetadata      agent's context linking data
     * @param userCustomAttributes user custom attributes
     * @param converseRequest      request
     * @param converseResponse     response
     */
    public ConverseModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes, ConverseRequest converseRequest,
            ConverseResponse converseResponse) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new ConverseModelRequest(converseRequest);
        this.modelResponse = new ConverseModelResponse(converseResponse);
    }

    /**
     * Construct a ConverseModelInvocation representing the
     * request and response for a streaming API.
     *
     * @param linkingMetadata             agent's context linking data
     * @param userCustomAttributes        user custom attributes
     * @param converseStreamRequest       stream request
     * @param converseStreamModelResponse response accumulated from the stream events
     * @param timeToFirstToken            duration in milliseconds from request start to the first token being streamed.
     */
    public ConverseModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes, ConverseStreamRequest converseStreamRequest,
            ConverseStreamModelResponse converseStreamModelResponse, long timeToFirstToken) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new ConverseModelStreamRequest(converseStreamRequest);
        try {
            this.timeToFirstToken = Math.toIntExact(timeToFirstToken);
        } catch (Exception e) {
            this.timeToFirstToken = 0;
            NewRelic.getAgent().getLogger().log(Level.WARNING, "AIM: The time_to_first_token value overflowed the maximum int size. Setting to 0 instead.");
        }
        this.modelResponse = converseStreamModelResponse;
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
                .responseModel() // uses request model in builder
                .responseNumberOfMessages(numberOfMessages)
                .responseChoicesFinishReason()
                .responseOrganization()
                .responseUsagePromptTokens()
                .responseUsageCompletionTokens()
                .responseUsageTotalTokens()
                .timeToFirstToken(timeToFirstToken)
                .error()
                .duration(System.currentTimeMillis() - startTime)
                .build();

        llmChatCompletionSummaryEvent.recordLlmChatCompletionSummaryEvent();
    }

    @Override
    public void recordLlmChatCompletionMessageEvent(int sequence, String message, String modelId, boolean isUser) {
        boolean hasCompleteUsage = LlmTokenCountResolver.hasCompleteUsageData(
                modelResponse.getResponseUsagePromptTokens(),
                modelResponse.getResponseUsageCompletionTokens(),
                modelResponse.getResponseUsageTotalTokens()
        );

        LlmEvent.Builder builder = new LlmEvent.Builder(this);

        LlmEvent llmChatCompletionMessageEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(getRandomGuid())
                .content(message)
                .role(isUser)
                .isResponse(isUser)
                .requestId()
                .responseModel() // uses request model in builder
                .sequence(sequence)
                .completionId()
                .tokenCount(LlmTokenCountResolver.getMessageTokenCount(hasCompleteUsage, modelId, message))
                .build();

        llmChatCompletionMessageEvent.recordLlmChatCompletionMessageEvent();
    }

    @Override
    public void recordLlmChatCompletionReasoningMessageEvent(int sequence, String reasoningContent, String signature, boolean redacted,
            String modelId) {
        boolean hasCompleteUsage = LlmTokenCountResolver.hasCompleteUsageData(
                modelResponse.getResponseUsagePromptTokens(),
                modelResponse.getResponseUsageCompletionTokens(),
                modelResponse.getResponseUsageTotalTokens()
        );

        LlmEvent.Builder builder = new LlmEvent.Builder(this);

        LlmEvent llmChatCompletionReasoningMessageEvent = builder
                .spanId()
                .traceId()
                .vendor()
                .ingestSource()
                .id(getRandomGuid())
                .reasoningContent(reasoningContent)
                .reasoningContentSignature(signature)
                .reasoningContentRedacted(redacted)
                .role(false)
                .isResponse(false)
                .requestId()
                .responseModel() // uses request model in builder
                .sequence(sequence)
                .completionId()
                .tokenCount(LlmTokenCountResolver.getMessageTokenCount(hasCompleteUsage, modelId, reasoningContent))
                .build();

        llmChatCompletionReasoningMessageEvent.recordLlmChatCompletionMessageEvent();
    }

    @Override
    public void recordLlmEvents(long startTime) {
        String operationType = modelResponse.getOperationType();
        if (operationType.equals(COMPLETION)) {
            recordLlmChatCompletionEvents(startTime);
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unexpected operation type encountered when trying to record LLM events");
        }
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
            recordLlmChatCompletionMessageEvent(sequence, modelRequest.getRequestMessage(i), modelRequest.getModelId(), modelRequest.isUser(i));
            sequence++;
        }

        // Second, record all LlmChatCompletionMessage events representing the completion message from the LLM response
        for (int i = 0; i < numberOfResponseMessages; i++) {
            // The ConverseResponse doesn't contain a model ID, so use the ConverseRequest model ID
            if (modelResponse.isReasoningMessage(i)) {
                recordLlmChatCompletionReasoningMessageEvent(sequence, modelResponse.getResponseReasoningContent(i),
                        modelResponse.getResponseReasoningSignature(i), modelResponse.isResponseReasoningRedacted(i), modelRequest.getModelId());
            } else {
                recordLlmChatCompletionMessageEvent(sequence, modelResponse.getResponseMessage(i), modelRequest.getModelId(), modelResponse.isUser());
            }
            sequence++;
        }

        // Finally, record a summary event representing all LlmChatCompletionMessage events
        recordLlmChatCompletionSummaryEvent(startTime, totalNumberOfMessages);
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
        // statusCode not available from ChatClientResponse
        int statusCode = modelResponse.getStatusCode();
        if (statusCode > 0) {
            errorParams.put("http.statusCode", statusCode);
            errorParams.put("error.code", statusCode);
        }
        if (!modelResponse.getLlmChatCompletionSummaryId().isEmpty()) {
            errorParams.put("completion_id", modelResponse.getLlmChatCompletionSummaryId());
        }
        // statusText not available from ChatClientResponse
        NewRelic.noticeError("LlmError: " + modelResponse.getStatusText(), errorParams);
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
