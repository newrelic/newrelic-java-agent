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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.HashMap;
import java.util.List;
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

    /**
     *
     * @param linkingMetadata      agent's context linking data
     * @param userCustomAttributes user custom attributes
     * @param chatClientRequest    a ChatClientRequest from a client call
     * @param chatClientResponse   a ChatClientResponse from a client call
     */
    public SpringAiModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes,
            ChatClientRequest chatClientRequest, ChatClientResponse chatClientResponse) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new SpringAiModelRequest(chatClientRequest);
        this.modelResponse = new SpringAiModelResponse(chatClientResponse, null);
    }

    /**
     * Takes a list of ChatClientResponse instances from a stream.
     *
     * @param linkingMetadata      agent's context linking data
     * @param userCustomAttributes user custom attributes
     * @param chatClientRequest    a ChatClientRequest from a stream call
     * @param list                 a list of ChatClientResponse from a client stream
     */
    public SpringAiModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes,
            ChatClientRequest chatClientRequest, List<ChatClientResponse> list) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new SpringAiModelRequest(chatClientRequest);
        int listSize = list.size();
        if (listSize > 0) {
            // pass in only the final ChatClientResponse instance from the stream contents
            // but with a list of Generations from all ChatClientResponse instances for the complete content
            this.modelResponse = new SpringAiModelResponse(list.get(listSize - 1), getStreamGeneration(list));
        } else {
            this.modelResponse = new SpringAiModelResponse(null, null);
        }
    }

    /**
     * Iterate through a List of ChatClientResponse instances to
     * construct the complete content string from all stream chunks.
     *
     * @param list List of ChatClientResponse instances returned as stream chunks
     * @return String representing the complete stream content
     */
    private String getStreamGeneration(
            List<ChatClientResponse> list) {
        StringBuilder content = new StringBuilder();
        if (list != null && !list.isEmpty()) {
            for (ChatClientResponse chatClientResponse : list) {
                ChatResponse chatResponse = chatClientResponse.chatResponse();
                if (chatResponse != null) {
                    for (Generation result : chatResponse.getResults()) {
                        AssistantMessage assistantMessage = result.getOutput();
                        if (assistantMessage != null) {
                            String messageText = assistantMessage.getText();
                            if (messageText != null) {
                                content.append(messageText);
                            }
                        }
                    }

                }
            }
        }
        return content.toString();
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

        LlmEvent llmEmbeddingEvent = builder // TODO setup embedding
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
    public void recordLlmChatCompletionMessageEvent(int sequence, String message, String modelId, boolean isUser) {
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
                .responseModel()
                .sequence(sequence)
                .completionId()
                .tokenCount(getTokenCount(modelId, message))
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
        // statusCode not available from ChatClientResponse
        int statusCode = modelResponse.getStatusCode();
        if (statusCode > 0) {
            errorParams.put("http.statusCode", statusCode);
            errorParams.put("error.code", statusCode);
        }
        if (!modelResponse.getLlmChatCompletionSummaryId().isEmpty()) {
            errorParams.put("completion_id", modelResponse.getLlmChatCompletionSummaryId());
        }
        if (!modelResponse.getLlmEmbeddingId().isEmpty()) {
            errorParams.put("embedding_id", modelResponse.getLlmEmbeddingId());
        }
        // statusText not available from ChatClientResponse
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
            recordLlmChatCompletionMessageEvent(sequence, modelRequest.getRequestMessage(i), modelRequest.getModelId(), modelRequest.isUser());
            sequence++;
        }

        // Second, record all LlmChatCompletionMessage events representing the completion message from the LLM response
        for (int i = 0; i < numberOfResponseMessages; i++) {
            recordLlmChatCompletionMessageEvent(sequence, modelResponse.getResponseMessage(i), modelResponse.getModelId(), modelResponse.isUser());
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
