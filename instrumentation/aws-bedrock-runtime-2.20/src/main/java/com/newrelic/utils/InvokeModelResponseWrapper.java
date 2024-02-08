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
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Stores the required info from the Bedrock InvokeModelResponse
 * but doesn't hold a reference to the actual response object.
 */
public class InvokeModelResponseWrapper {
    private final String invokeModelResponseBody;
    private Map<String, JsonNode> responseBodyJsonMap = null;

    // Response body (for Claude, how about other models?)
    public static final String COMPLETION = "completion";
    public static final String EMBEDDING = "embedding";
    private static final String STOP_REASON = "stop_reason";
    private static final String STOP = "stop";

    // Response headers
    private static final String X_AMZN_BEDROCK_INPUT_TOKEN_COUNT = "X-Amzn-Bedrock-Input-Token-Count";
    private static final String X_AMZN_BEDROCK_OUTPUT_TOKEN_COUNT = "X-Amzn-Bedrock-Output-Token-Count";
    private static final String X_AMZN_REQUEST_ID = "x-amzn-RequestId";
    private static final String X_AMZN_BEDROCK_INVOCATION_LATENCY = "X-Amzn-Bedrock-Invocation-Latency";
    private String inputTokenCount;
    private String outputTokenCount;
    private String amznRequestId;
    private String invocationLatency;

    // LLM operation type
    private String operationType;

    public InvokeModelResponseWrapper(InvokeModelResponse invokeModelResponse) {
        if (invokeModelResponse != null) {
            invokeModelResponseBody = invokeModelResponse.body().asUtf8String();
            extractOperationType(invokeModelResponseBody);
            extractHeaders(invokeModelResponse);
        } else {
            invokeModelResponseBody = "";
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Received null InvokeModelResponse");
        }
    }

    private void extractOperationType(String invokeModelResponseBody) {
        // FIXME should be starts with instead of contains?
        if (!invokeModelResponseBody.isEmpty()) {
            if (invokeModelResponseBody.contains(COMPLETION)) {
                operationType = COMPLETION;
            } else if (invokeModelResponseBody.contains(EMBEDDING)) {
                operationType = EMBEDDING;
            }
        }
    }

    private void extractHeaders(InvokeModelResponse invokeModelResponse) {
        Map<String, List<String>> headers = invokeModelResponse.sdkHttpResponse().headers();
        if (!headers.isEmpty()) {
            List<String> inputTokenCountHeaders = headers.get(X_AMZN_BEDROCK_INPUT_TOKEN_COUNT);
            if (inputTokenCountHeaders != null && !inputTokenCountHeaders.isEmpty()) {
                inputTokenCount = inputTokenCountHeaders.get(0);
            }
            List<String> outputTokenCountHeaders = headers.get(X_AMZN_BEDROCK_OUTPUT_TOKEN_COUNT);
            if (outputTokenCountHeaders != null && !outputTokenCountHeaders.isEmpty()) {
                outputTokenCount = outputTokenCountHeaders.get(0);
            }
            List<String> amznRequestIdHeaders = headers.get(X_AMZN_REQUEST_ID);
            if (amznRequestIdHeaders != null && !amznRequestIdHeaders.isEmpty()) {
                amznRequestId = amznRequestIdHeaders.get(0); // TODO does this differ from invokeModelResponse.responseMetadata().requestId()
            }
            List<String> invocationLatencyHeaders = headers.get(X_AMZN_BEDROCK_INVOCATION_LATENCY);
            if (invocationLatencyHeaders != null && !invocationLatencyHeaders.isEmpty()) {
                invocationLatency = invocationLatencyHeaders.get(0);
            }
        }
    }

    // Lazy init and only parse map once
    public Map<String, JsonNode> getResponseBodyJsonMap() {
        if (responseBodyJsonMap == null) {
            responseBodyJsonMap = parseInvokeModelResponseBodyMap();
        }
        return responseBodyJsonMap;
    }

    private Map<String, JsonNode> parseInvokeModelResponseBodyMap() {
        // Use AWS SDK JSON parsing to parse response body
        JsonNodeParser jsonNodeParser = JsonNodeParser.create();
        JsonNode responseBodyJsonNode = jsonNodeParser.parse(invokeModelResponseBody);

        Map<String, JsonNode> responseBodyJsonMap = null;
        // TODO check for other types? Or will it always be Object?
        if (responseBodyJsonNode != null && responseBodyJsonNode.isObject()) {
            responseBodyJsonMap = responseBodyJsonNode.asObject();
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse InvokeModelResponse body as Map Object");
        }

        return responseBodyJsonMap != null ? responseBodyJsonMap : Collections.emptyMap();
    }

    public String parseCompletion() {
        String completion = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(COMPLETION);
                if (jsonNode.isString()) {
                    completion = jsonNode.asString();
                }
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse " + COMPLETION);
        }
        return completion;
    }

    public String parseStopReason() {
        String stopReason = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(STOP_REASON);
                if (jsonNode.isString()) {
                    stopReason = jsonNode.asString();
                }
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse " + STOP_REASON);
        }
        return stopReason;
    }

    public String parseStop() {
        String stop = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(STOP);
                if (jsonNode.isString()) {
                    stop = jsonNode.asString();
                }
            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse " + STOP);
        }
        return stop.replaceAll("[\n:]", "");
    }

    public String getInputTokenCount() {
        return inputTokenCount;
    }

    public String getOutputTokenCount() {
        return outputTokenCount;
    }

    public String getAmznRequestId() {
        return amznRequestId;
    }

    public String getInvocationLatency() {
        return invocationLatency;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getResponseModel() {
        // TODO figure out where to get this from
        return "TODO";
    }
}
