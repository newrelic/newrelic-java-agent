/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.api.agent.NewRelic;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Stores the required info from the Bedrock InvokeModelRequest
 * but doesn't hold a reference to the actual request object.
 */
public class InvokeModelRequestWrapper {
    private final String invokeModelRequestBody;
    private final String modelId;
    private Map<String, JsonNode> requestBodyJsonMap = null;

    // Request body (for Claude, how about other models?)
    private static final String STOP_SEQUENCES = "stop_sequences";
    private static final String MAX_TOKENS_TO_SAMPLE = "max_tokens_to_sample";
    private static final String TEMPERATURE = "temperature";
    private static final String PROMPT = "prompt";

    public InvokeModelRequestWrapper(InvokeModelRequest invokeModelRequest) {
        if (invokeModelRequest != null) {
            invokeModelRequestBody = invokeModelRequest.body().asUtf8String();
            modelId = invokeModelRequest.modelId();
        } else {
            invokeModelRequestBody = "";
            modelId = "";
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Received null InvokeModelRequest");
        }
    }

    // Lazy init and only parse map once
    public Map<String, JsonNode> getRequestBodyJsonMap() {
        if (requestBodyJsonMap == null) {
            requestBodyJsonMap = parseInvokeModelRequestBodyMap();
        }
        return requestBodyJsonMap;
    }

    private Map<String, JsonNode> parseInvokeModelRequestBodyMap() {
        // Use AWS SDK JSON parsing to parse request body
        JsonNodeParser jsonNodeParser = JsonNodeParser.create();
        JsonNode requestBodyJsonNode = jsonNodeParser.parse(invokeModelRequestBody);

        Map<String, JsonNode> requestBodyJsonMap = null;
        // TODO check for other types? Or will it always be Object?
        if (requestBodyJsonNode != null && requestBodyJsonNode.isObject()) {
            requestBodyJsonMap = requestBodyJsonNode.asObject();
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse InvokeModelRequest body as Map Object");
        }

        return requestBodyJsonMap != null ? requestBodyJsonMap : Collections.emptyMap();
    }

    // TODO do we potentially expect more than one entry in the stop sequence? Or is it sufficient
    //  to just check if it contains Human?
    public String parseStopSequences() {
        StringBuilder stopSequences = new StringBuilder();
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(STOP_SEQUENCES);
                if (jsonNode.isArray()) {
                    List<JsonNode> jsonNodeArray = jsonNode.asArray();
                    for (JsonNode node : jsonNodeArray) {
                        if (node.isString()) {
                            // Don't add comma for first node
                            if (stopSequences.length() <= 0) {
                                stopSequences.append(node.asString());
                            } else {
                                stopSequences.append(",").append(node.asString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse " + STOP_SEQUENCES);
        }
        return stopSequences.toString().replaceAll("[\n:]", "");
    }

    public String parseMaxTokensToSample() {
        String maxTokensToSample = "";
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(MAX_TOKENS_TO_SAMPLE);
                if (jsonNode.isNumber()) {
                    maxTokensToSample = jsonNode.asNumber();
                }
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse " + MAX_TOKENS_TO_SAMPLE);
        }
        return maxTokensToSample;
    }

    public String parseTemperature() {
        String temperature = "";
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(TEMPERATURE);
                if (jsonNode.isNumber()) {
                    temperature = jsonNode.asNumber();
                }
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse " + TEMPERATURE);
        }
        return temperature;
    }

    public String parsePrompt() {
        String prompt = "";
        try {
            if (!getRequestBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getRequestBodyJsonMap().get(PROMPT);
                if (jsonNode.isString()) {
                    prompt = jsonNode.asString();
                }
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse " + PROMPT);
        }
        return prompt.replace("Human: ", "").replace("\n\nAssistant:", "");
    }

    public String getModelId() {
        return modelId;
    }
}
