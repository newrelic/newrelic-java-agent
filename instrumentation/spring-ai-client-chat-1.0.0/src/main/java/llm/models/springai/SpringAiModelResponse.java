/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.springai;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelResponse;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static llm.models.ModelInvocation.getRandomGuid;
import static llm.models.ModelResponse.logParsingFailure;

/**
 * Stores the required info from the SpringAI ChatClientResponse without holding
 * a reference to the actual request object to avoid potential memory issues.
 */
public class SpringAiModelResponse implements ModelResponse {
    private static final String EMBEDDINGS = "embeddings";
    private static final String ASSISTANT = "assistant";

    // LLM operation type
    private String operationType = "";
    private int statusCode = 0;
    private String statusText = "";
    private String responseOrganization = "";
    private String llmChatCompletionSummaryId = "";
    private String llmEmbeddingId = "";
    private int promptTokens = 0;
    private int completionTokens = 0;
    private int totalTokens = 0;
    private int timeToFirstToken = 0;
    private String model = "";
    private String requestId = "";
    private String stopReason = "";
    private String messageTypeValue = "";
    private List<Generation> responseGenerations = new ArrayList<>();

    public SpringAiModelResponse(ChatClientResponse chatClientResponse, String streamGeneration) {
        if (chatClientResponse != null) {
            ChatResponse chatResponse = chatClientResponse.chatResponse();
            if (chatResponse != null) {
                if (streamGeneration != null) {
                    responseGenerations.add(new Generation(new AssistantMessage(streamGeneration)));
                } else {
                    List<Generation> results = chatResponse.getResults();
                    if (results != null) {
                        responseGenerations.addAll(results);
                    }
                }
                Generation result = chatResponse.getResult();
                if (result != null) {
                    AssistantMessage output = result.getOutput();
                    if (output != null) {
                        MessageType messageType = output.getMessageType();
                        if (messageType != null) {
                            messageTypeValue = messageType.getValue();
                        }
                    }
                    ChatGenerationMetadata chatGenerationMetadata = result.getMetadata();
                    if (chatGenerationMetadata != null) {
                        String finishReason = chatGenerationMetadata.getFinishReason();
                        if (finishReason != null) {
                            stopReason = finishReason;
                        }
                    }
                }

                ChatResponseMetadata chatResponseMetadata = chatResponse.getMetadata();
                if (chatResponseMetadata != null) {
                    Usage usage = chatResponseMetadata.getUsage();
                    if (usage != null) {
                        promptTokens = usage.getPromptTokens();
                        completionTokens = usage.getCompletionTokens();
                        totalTokens = usage.getTotalTokens();
                    }
                    model = chatResponseMetadata.getModel();
                    // A unique identifier for the chat completion operation.
                    llmChatCompletionSummaryId = chatResponseMetadata.getId();
                }
            }

            llmEmbeddingId = getRandomGuid(); // TODO does this exist? need to debug embedding
            requestId = getRandomGuid();
            setOperationType(llmChatCompletionSummaryId);
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Received null SpringAI ChatClientResponse");
        }
    }

    /**
     * Parses the operation type from the chat completion ID and assigns it to a field.
     *
     * @param llmChatCompletionSummaryId A unique identifier for the chat completion operation
     */
    private void setOperationType(String llmChatCompletionSummaryId) {
        try {
            if (!llmChatCompletionSummaryId.isEmpty()) { // FIXME does requestId make sense???
                if (llmChatCompletionSummaryId.contains("chatcmpl")) {
                    operationType = COMPLETION;
                } else if (llmChatCompletionSummaryId.contains(EMBEDDINGS)) { // FIXME debug embedding
                    operationType = EMBEDDING;
                } else {
                    logParsingFailure(null, "operation type");
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, "operation type");
        }
    }

    @Override
    public String getResponseMessage(int index) {
        Generation generation = responseGenerations.get(index);
        AssistantMessage assistantMessage = generation.getOutput();
        if (assistantMessage != null) {
            return assistantMessage.getText();
        }
        return "";
    }

    @Override
    public int getNumberOfResponseMessages() {
        return responseGenerations.size();
    }

    @Override
    public String getStopReason() {
        return stopReason;
    }

    @Override
    public String getRequestId() {
        // Not available
        return requestId;
    }

    @Override
    public String getOperationType() {
        return operationType;
    }

    @Override
    public String getLlmChatCompletionSummaryId() {
        return llmChatCompletionSummaryId != null ? llmChatCompletionSummaryId : getRandomGuid();
    }

    @Override
    public String getLlmEmbeddingId() {
        return llmEmbeddingId; // TODO need to debug embedding
    }

    @Override
    public boolean isErrorResponse() {
        /*
         * The ChatClientResponse doesn't seem to include any info about errors.
         * When a ChatClient request in Spring AI fails, the error response is typically
         * encapsulated in a Spring RestClientException or a similar exception type
         * within the Spring framework, such as WebClientRequestException (for streaming calls)
         * or an IOException related to connection issues.
         */
        return false;
    }

    @Override
    public int getStatusCode() {
        // Not available
        return statusCode;
    }

    @Override
    public String getStatusText() {
        // Not available
        return statusText;
    }

    @Override
    public String getModelId() {
        return model;
    }

    @Override
    public String getResponseOrganization() {
        // Not available
        return responseOrganization;
    }

    @Override
    public Integer getResponseUsageTotalTokens() {
        return totalTokens;
    }

    @Override
    public Integer getResponseUsagePromptTokens() {
        return promptTokens;
    }

    @Override
    public Integer getResponseUsageCompletionTokens() {
        return completionTokens;
    }

    @Override
    public Integer getTimeToFirstToken() {
        // This only applies to streams
        return timeToFirstToken; // TODO this is from streams only
    }

    @Override
    public boolean isUser() {
        return !ASSISTANT.equalsIgnoreCase(messageTypeValue);
    }
}
