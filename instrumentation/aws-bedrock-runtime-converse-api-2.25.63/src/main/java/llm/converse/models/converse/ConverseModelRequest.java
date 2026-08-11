/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.converse.models.converse;

import com.newrelic.api.agent.NewRelic;
import llm.converse.models.ModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Stores the required info from the Bedrock ConverseRequest.
 * Avoids holding a reference to the request object to prevent potential memory issues.
 */
public class ConverseModelRequest implements ModelRequest {
    private String modelId = "";
    private Integer maxTokens = 0;
    private Float temperature = 0.0F;
    private int numOfMessages = 0;
    private List<Message> messages = new ArrayList<>();

    public ConverseModelRequest(ConverseRequest converseRequest) {
        if (converseRequest != null) {
            modelId = converseRequest.modelId();
            InferenceConfiguration inferenceConfiguration = converseRequest.inferenceConfig();

            if (inferenceConfiguration != null) {
                maxTokens = inferenceConfiguration.maxTokens();
                temperature = inferenceConfiguration.temperature();
            }
            if (converseRequest.hasMessages()) {
                messages = converseRequest.messages();
                numOfMessages = messages.size();
            }
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Received null ConverseRequest");
        }
    }

    @Override
    public int getMaxTokensToSample() {
        return maxTokens;
    }

    @Override
    public float getTemperature() {
        return temperature;
    }

    @Override
    public int getNumberOfRequestMessages() {
        return numOfMessages;
    }

    @Override
    public String getRequestMessage(int index) {
        // Request message for chat completion request
        Message message = messages.get(index);
        StringBuilder messageBuilder = new StringBuilder();
        if (message != null && message.hasContent()) {
            for (ContentBlock contentBlock : message.content()) {
                messageBuilder.append(contentBlock.text());
            }
        }
        return messageBuilder.toString();
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public boolean isUser(int index) {
        return messages.get(index).role().toString().equalsIgnoreCase("user");
    }
}
