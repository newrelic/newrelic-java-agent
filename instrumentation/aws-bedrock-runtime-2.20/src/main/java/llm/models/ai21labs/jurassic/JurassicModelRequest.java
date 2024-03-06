/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.ai21labs.jurassic;

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
public class JurassicModelRequest implements ModelRequest {
    private static final String MAX_TOKENS = "maxTokens";
    private static final String TEMPERATURE = "temperature";
    private static final String PROMPT = "prompt";
    private static final String INPUT_TEXT = "inputText";

    private String invokeModelRequestBody = "";
    private String modelId = "";
    private Map<String, JsonNode> requestBodyJsonMap = null;

    public JurassicModelRequest(InvokeModelRequest invokeModelRequest) {
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
                JsonNode jsonNode = getRequestBodyJsonMap().get(MAX_TOKENS);
                if (jsonNode.isNumber()) {
                    String maxTokensToSampleString = jsonNode.asNumber();
                    maxTokensToSample = Integer.parseInt(maxTokensToSampleString);
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, MAX_TOKENS);
        }
        if (maxTokensToSample == 0) {
            logParsingFailure(null, MAX_TOKENS);
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
        return parseStringValue(PROMPT);
    }

    @Override
    public String getRole() {
        // This is a NoOp for Jurassic as the request doesn't contain any signifier of the role
        return "";
    }

    @Override
    public String getInputText() {
        return parseStringValue(INPUT_TEXT);
    }

    private String parseStringValue(String fieldToParse) {
        String parsedStringValue = "";
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(fieldToParse);
                if (jsonNode.isString()) {
                    parsedStringValue = jsonNode.asString();
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, fieldToParse);
        }
        if (parsedStringValue.isEmpty()) {
            logParsingFailure(null, fieldToParse);
        }
        return parsedStringValue;
    }

    @Override
    public String getModelId() {
        return modelId;
    }
}
