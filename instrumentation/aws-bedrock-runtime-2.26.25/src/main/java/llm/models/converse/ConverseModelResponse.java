/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.converse;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static llm.models.ModelInvocation.getRandomGuid;

/**
 * Stores the required info from the Bedrock ConverseResponse.
 * Avoids holding a reference to the response object to prevent potential memory issues.
 */
public class ConverseModelResponse implements ModelResponse {
    private String amznRequestId = "";
    private String responseOrganization = "";

    // LLM operation type
    private String operationType = "";

    // HTTP response
    private boolean isSuccessfulResponse = false;
    private int statusCode = 0;
    private String statusText = "";

    private String llmChatCompletionSummaryId = "";
    private String stopReason = "";
    private String role = "";
    private List<ContentBlock> contentList = new ArrayList<>();
    private Integer inputTokens = 0;
    private Integer outputTokens = 0;
    private Integer totalTokens = 0;

    public ConverseModelResponse(ConverseResponse converseResponse) {
        if (converseResponse != null) {
            // Converse APIs do not support embedding operations, only chat completions
            operationType = COMPLETION;
            SdkHttpResponse sdkHttpResponse = converseResponse.sdkHttpResponse();
            if (sdkHttpResponse != null) {
                isSuccessfulResponse = sdkHttpResponse.isSuccessful();
                statusCode = sdkHttpResponse.statusCode();
                Optional<String> statusTextOptional = sdkHttpResponse.statusText();
                statusTextOptional.ifPresent(s -> statusText = s);
            }
            amznRequestId = converseResponse.responseMetadata().requestId();
            stopReason = converseResponse.stopReasonAsString();
            Message message = converseResponse.output().message();
            if (message.hasContent()) {
                role = message.role().toString();
                contentList = message.content();
            }
            TokenUsage usage = converseResponse.usage();
            if (usage != null) {
                inputTokens = usage.inputTokens();
                outputTokens = usage.outputTokens();
                totalTokens = usage.totalTokens();
            }
            llmChatCompletionSummaryId = getRandomGuid();
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Received null ConverseResponse");
        }
    }

    @Override
    public String getResponseMessage(int index) {
        // Response message for chat completion request
        StringBuilder messageBuilder = new StringBuilder();
        for (ContentBlock contentBlock : contentList) {
            messageBuilder.append(contentBlock.text());
        }
        return messageBuilder.toString();
    }

    @Override
    public int getNumberOfResponseMessages() {
        return contentList.size();
    }

    @Override
    public String getStopReason() {
        return stopReason;
    }

    @Override
    public String getAmznRequestId() {
        return amznRequestId;
    }

    @Override
    public String getOperationType() {
        return operationType;
    }

    @Override
    public String getLlmChatCompletionSummaryId() {
        return llmChatCompletionSummaryId;
    }

    @Override
    public boolean isErrorResponse() {
        return !isSuccessfulResponse;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getStatusText() {
        return statusText;
    }

    @Override
    public boolean isUser() {
        return role.equalsIgnoreCase("user");
    }

    @Override
    public String getResponseOrganization() {
        // Not available
        return responseOrganization;
    }

    @Override
    public Integer getResponseUsagePromptTokens() {
        return inputTokens;
    }

    @Override
    public Integer getResponseUsageCompletionTokens() {
        return outputTokens;
    }

    @Override
    public Integer getResponseUsageTotalTokens() {
        return totalTokens;
    }
}
