/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.anthropic.claude;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelRequest;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import static llm.models.ModelRequest.logParsingFailure;

/**
 * Stores the required info from the Bedrock InvokeModelRequest without holding
 * a reference to the actual request object to avoid potential memory issues.
 */
public class AnthropicClaudeInvokeModelRequest implements ModelRequest {
    // TODO might be able to move some of these constants to the ModelRequest interface
    //  need to figure out if they are consistent across all models
    private static final String MAX_TOKENS_TO_SAMPLE = "max_tokens_to_sample";
    private static final String TEMPERATURE = "temperature";
    private static final String PROMPT = "prompt";
    private static final String INPUT_TEXT = "inputText";
    private static final String ESCAPED_NEWLINES = "\\n\\n";
    private static final String SYSTEM = "system";
    private static final String ASSISTANT = "assistant";
    private static final String USER = "user";

    private String invokeModelRequestBody = "";
    private String modelId = "";
    private Map<String, JsonNode> requestBodyJsonMap = null;

    public AnthropicClaudeInvokeModelRequest(InvokeModelRequest invokeModelRequest) {
        if (invokeModelRequest != null) {
            invokeModelRequestBody = invokeModelRequest.body().asUtf8String();
            modelId = invokeModelRequest.modelId();
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Received null InvokeModelRequest");
        }
    }

    /**
     * Get a map of the Request body contents.
     * <p>
     * Use this method to obtain the Request body contents so that the map is lazily initialized and only parsed once.
     *
     * @return map of String to JsonNode
     */
    private Map<String, JsonNode> getRequestBodyJsonMap() {
        if (requestBodyJsonMap == null) {
            requestBodyJsonMap = parseInvokeModelRequestBodyMap();
        }
        return requestBodyJsonMap;
    }

    /**
     * Convert JSON Request body string into a map.
     *
     * @return map of String to JsonNode
     */
    private Map<String, JsonNode> parseInvokeModelRequestBodyMap() {
        // Use AWS SDK JSON parsing to parse request body
        JsonNodeParser jsonNodeParser = JsonNodeParser.create();
        JsonNode requestBodyJsonNode = jsonNodeParser.parse(invokeModelRequestBody);

        Map<String, JsonNode> requestBodyJsonMap = null;
        try {
            if (requestBodyJsonNode != null && requestBodyJsonNode.isObject()) {
                requestBodyJsonMap = requestBodyJsonNode.asObject();
            } else {
                logParsingFailure(null, "request body");
            }
        } catch (Exception e) {
            logParsingFailure(e, "request body");
        }
        return requestBodyJsonMap != null ? requestBodyJsonMap : Collections.emptyMap();
    }

    @Override
    public int getMaxTokensToSample() {
        int maxTokensToSample = 0;
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(MAX_TOKENS_TO_SAMPLE);
                if (jsonNode.isNumber()) {
                    String maxTokensToSampleString = jsonNode.asNumber();
                    maxTokensToSample = Integer.parseInt(maxTokensToSampleString);
                }
            } else {
                logParsingFailure(null, MAX_TOKENS_TO_SAMPLE);
            }
        } catch (Exception e) {
            logParsingFailure(e, MAX_TOKENS_TO_SAMPLE);
        }
        return maxTokensToSample;
    }

    @Override
    public float getTemperature() {
        float temperature = 0f;
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(TEMPERATURE);
                if (jsonNode.isNumber()) {
                    String temperatureString = jsonNode.asNumber();
                    temperature = Float.parseFloat(temperatureString);
                }
            } else {
                logParsingFailure(null, TEMPERATURE);
            }
        } catch (Exception e) {
            logParsingFailure(e, TEMPERATURE);
        }
        return temperature;
    }

    @Override
    public String getRequestMessage() {
        String prompt = "";
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(PROMPT);
                if (jsonNode.isString()) {
                    prompt = jsonNode.asString();
                }
            } else {
                logParsingFailure(null, PROMPT);
            }
        } catch (Exception e) {
            logParsingFailure(e, PROMPT);
        }
        return prompt;
    }

    @Override
    public String getRole() {
        try {
            if (!invokeModelRequestBody.isEmpty()) {
                String invokeModelRequestBodyLowerCase = invokeModelRequestBody.toLowerCase();
                if (invokeModelRequestBodyLowerCase.contains(ESCAPED_NEWLINES + SYSTEM)) {
                    return SYSTEM;
                } else if (invokeModelRequestBodyLowerCase.contains(ESCAPED_NEWLINES + USER)) {
                    return USER;
                } else if (invokeModelRequestBodyLowerCase.contains(ESCAPED_NEWLINES + ASSISTANT)) {
                    return ASSISTANT;
                }
            } else {
                logParsingFailure(null, "role");
            }
        } catch (Exception e) {
            logParsingFailure(e, "role");
        }
        return "";
    }

    @Override
    public String getInputText() {
        String inputText = "";
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(INPUT_TEXT);
                if (jsonNode.isString()) {
                    inputText = jsonNode.asString();
                }
            } else {
                logParsingFailure(null, INPUT_TEXT);
            }
        } catch (Exception e) {
            logParsingFailure(e, INPUT_TEXT);
        }
        return inputText;
    }

    @Override
    public String getModelId() {
        return modelId;
    }
}
