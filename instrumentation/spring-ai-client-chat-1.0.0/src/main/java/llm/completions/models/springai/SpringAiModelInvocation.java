/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.completions.models.springai;

import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountResolver;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import llm.completions.events.LlmEvent;
import llm.completions.models.ModelInvocation;
import llm.completions.models.ModelRequest;
import llm.completions.models.ModelResponse;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static llm.completions.models.ModelInvocation.getRandomGuid;
import static llm.completions.models.ModelResponse.COMPLETION;
import static llm.completions.vendor.Vendor.SPRING_AI;

public class SpringAiModelInvocation implements ModelInvocation {
    Map<String, String> linkingMetadata;
    Map<String, Object> userAttributes;
    ModelRequest modelRequest;
    ModelResponse modelResponse;
    int timeToFirstToken;

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
            ChatClientRequest chatClientRequest, List<ChatClientResponse> list, long timeToFirstToken) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new SpringAiModelRequest(chatClientRequest);
        try {
            this.timeToFirstToken = Math.toIntExact(timeToFirstToken);
        } catch (ArithmeticException e) {
            this.timeToFirstToken = 0;
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: The time_to_first_token value overflowed the maximum int size. Setting to 0 instead.");
        }
        int listSize = list.size();
        if (listSize > 0) {
            // pass in only the ChatClientResponse instance from the stream contents but with
            // a list of Generations from all ChatClientResponse instances for the complete content

            // This is kind of gross. The behavior with streamed results can vary based on how the client is configured for different LLM servers. In the default case, for OpenAI, the final stream chunk will have the ChatClientResponse instance that contains the info that we want. For some streaming APIs, like OpenAI’s, there is extra config that can be set by the client that causes token usage to be sent back from the LLM server (it's not by default). In this case, the final stream chunk will only contain the token usage stats and none of the other required info, so we actually need to process the second to last chunk.
            ChatClientResponse chatClientResponseLastChunk = list.get(listSize - 1);
            if (hasFinishReason(chatClientResponseLastChunk)) {
                this.modelResponse = new SpringAiModelResponse(chatClientResponseLastChunk, getStreamGeneration(list));
            } else if (listSize > 1) {
                ChatClientResponse chatClientResponseSecondToLastChunk = list.get(listSize - 2);
                this.modelResponse = new SpringAiModelResponse(chatClientResponseSecondToLastChunk, getStreamGeneration(list));
            } else {
                // Just fallback to last chunk if neither of the above cases are true
                this.modelResponse = new SpringAiModelResponse(chatClientResponseLastChunk, getStreamGeneration(list));
            }
        } else {
            this.modelResponse = new SpringAiModelResponse(null, null);
        }
    }

    /**
     * Check if the ChatClientResponse has a finish reason
     *
     * @param chatClientResponse instance to inspect
     * @return true if there is a finish reason, else false
     */
    private boolean hasFinishReason(ChatClientResponse chatClientResponse) {
        if (chatClientResponse != null) {
            ChatResponse chatResponse = chatClientResponse.chatResponse();
            if (chatResponse != null) {
                Generation result = chatResponse.getResult();
                if (result != null) {
                    ChatGenerationMetadata chatGenerationMetadata = result.getMetadata();
                    if (chatGenerationMetadata != null) {
                        return chatGenerationMetadata.getFinishReason() != null;
                    }
                }
            }
        }
        return false;
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
                .responseModel()
                .sequence(sequence)
                .completionId()
                .tokenCount(LlmTokenCountResolver.getMessageTokenCount(hasCompleteUsage, modelId, message))
                .build();

        llmChatCompletionMessageEvent.recordLlmChatCompletionMessageEvent();
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
