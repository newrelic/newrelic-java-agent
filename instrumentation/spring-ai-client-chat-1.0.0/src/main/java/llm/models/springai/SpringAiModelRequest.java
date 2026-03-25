/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.springai;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelRequest;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.logging.Level;

/**
 * Stores the required info from the SpringAI ChatClientRequest without holding
 * a reference to the actual request object to avoid potential memory issues.
 */
public class SpringAiModelRequest implements ModelRequest {
    private static final String USER = "user";
    private String promptUserMessage = "";
    private String modelId = "";
    private Integer maxTokensToSample = 0;
    private float temperature = 0;
    private int numberOfRequestMessages = 0;
    private String messageTypeValue = "";

    public SpringAiModelRequest(ChatClientRequest chatClientRequest) {
        if (chatClientRequest != null) {
            Prompt prompt = chatClientRequest.prompt();
            if (prompt != null) {
                ChatOptions chatOptions = prompt.getOptions();
                if (chatOptions != null) {
                    modelId = chatOptions.getModel();

                    Integer maxTokens = chatOptions.getMaxTokens();
                    if (maxTokens != null && maxTokens > 0) {
                        maxTokensToSample = maxTokens;
                    }

                    Double temp = chatOptions.getTemperature();
                    if (temp != null && temp > 0.0) {
                        temperature = temp.floatValue();
                    }
                }

                List<UserMessage> userMessages = prompt.getUserMessages();
                if (userMessages != null) {
                    numberOfRequestMessages = userMessages.size();
                }

                UserMessage userMessage = prompt.getUserMessage();
                if (userMessage != null) {
                    promptUserMessage = userMessage.getText();
                    MessageType messageType = userMessage.getMessageType();
                    if (messageType != null) {
                        messageTypeValue = messageType.getValue();
                    }
                }
            } else {
                NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Received null SpringAI ChatClientRequest");
            }
        }
    }

    @Override
    public int getMaxTokensToSample() {
        return maxTokensToSample;
    }

    @Override
    public float getTemperature() {
        return temperature;
    }

    @Override
    public int getNumberOfRequestMessages() {
        return numberOfRequestMessages;
    }

    @Override
    public String getRequestMessage(int index) {
        return promptUserMessage;
    }

    @Override
    public String getInputText(int index) {
        return ""; // TODO debug embedding
    }

    @Override
    public int getNumberOfInputTextMessages() {
        return 0; // TODO debug embedding
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public boolean isUser() {
        return USER.equalsIgnoreCase(messageTypeValue);
    }
}
