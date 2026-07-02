/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.converse;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

// TODO nothing in this class has been tested as streaming support isn't yet implemented. It seems that
//  ConverseStreamRequest provides the same APIs as ConverseRequest, so perhaps this class isn't necessary
//  and ConverseModelRequest can just handle both request types (ConverseStreamRequest and ConverseRequest).
//  Might just need to modify the constructor of ConverseModelRequest to accept a more generic
//  shared interface/abstract class (e.g. BedrockRuntimeRequest, AwsRequest, or SdkRequest) and
//  just cast to the appropriate request type if need be.

/**
 * Stores the required info from the Bedrock ConverseRequest.
 * Avoids holding a reference to the request object to prevent potential memory issues.
 */
public class ConverseModelStreamRequest implements ModelRequest {
    private String modelId = "";
    private Integer maxTokens = 0;
    private Float temperature = 0.0F;
    private int numOfMessages = 0;
    private List<Message> messages = new ArrayList<>();

    public ConverseModelStreamRequest(ConverseStreamRequest converseStreamRequest) {
        if (converseStreamRequest != null) {
            modelId = converseStreamRequest.modelId();
            InferenceConfiguration inferenceConfiguration = converseStreamRequest.inferenceConfig();

            if (inferenceConfiguration != null) {
                maxTokens = inferenceConfiguration.maxTokens();
                temperature = inferenceConfiguration.temperature();
            }
            if (converseStreamRequest.hasMessages()) {
                messages = converseStreamRequest.messages();
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
