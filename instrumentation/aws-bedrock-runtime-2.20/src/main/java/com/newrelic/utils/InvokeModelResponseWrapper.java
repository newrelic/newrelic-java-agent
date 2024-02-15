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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores the required info from the Bedrock InvokeModelResponse
 * but doesn't hold a reference to the actual response object.
 */
public class InvokeModelResponseWrapper {
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
    private int inputTokenCount = 0;
    private int outputTokenCount = 0;
    private String amznRequestId = "";
    private String invocationLatency = "";

    // LLM operation type
    private String operationType = "";

    // HTTP response
    private boolean isSuccessfulResponse = false;
    private int statusCode = 0;
    private String statusText = "";

    // Random GUID for response
    private String llmChatCompletionMessageId = "";
    private String llmChatCompletionSummaryId = "";
    private String llmEmbeddingId = "";

    private String invokeModelResponseBody = "";
    private Map<String, JsonNode> responseBodyJsonMap = null;

    private static final String JSON_START = "{\"";

    public InvokeModelResponseWrapper(InvokeModelResponse invokeModelResponse) {
        if (invokeModelResponse != null) {
            invokeModelResponseBody = invokeModelResponse.body().asUtf8String();
            isSuccessfulResponse = invokeModelResponse.sdkHttpResponse().isSuccessful();
            statusCode = invokeModelResponse.sdkHttpResponse().statusCode();
            Optional<String> statusTextOptional = invokeModelResponse.sdkHttpResponse().statusText();
            statusTextOptional.ifPresent(s -> statusText = s);
            setOperationType(invokeModelResponseBody);
            setHeaderFields(invokeModelResponse);
            llmChatCompletionMessageId = UUID.randomUUID().toString();
            llmChatCompletionSummaryId = UUID.randomUUID().toString();
            llmEmbeddingId = UUID.randomUUID().toString();
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Received null InvokeModelResponse");
        }
    }

    /**
     * Get a map of the Response body contents.
     * <p>
     * Use this method to obtain the Response body contents so that the map is lazily initialized and only parsed once.
     *
     * @return map of String to JsonNode
     */
    private Map<String, JsonNode> getResponseBodyJsonMap() {
        if (responseBodyJsonMap == null) {
            responseBodyJsonMap = parseInvokeModelResponseBodyMap();
        }
        return responseBodyJsonMap;
    }

    /**
     * Convert JSON Response body string into a map.
     *
     * @return map of String to JsonNode
     */
    private Map<String, JsonNode> parseInvokeModelResponseBodyMap() {
        Map<String, JsonNode> responseBodyJsonMap = null;
        try {
            // Use AWS SDK JSON parsing to parse response body
            JsonNodeParser jsonNodeParser = JsonNodeParser.create();
            JsonNode responseBodyJsonNode = jsonNodeParser.parse(invokeModelResponseBody);

            // TODO check for other types? Or will it always be Object?
            if (responseBodyJsonNode != null && responseBodyJsonNode.isObject()) {
                responseBodyJsonMap = responseBodyJsonNode.asObject();
            }
//            else {
//                NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse InvokeModelResponse body as Map Object");
//            }
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse InvokeModelResponse body as Map Object");
        }

        return responseBodyJsonMap != null ? responseBodyJsonMap : Collections.emptyMap();
    }

    private void setOperationType(String invokeModelResponseBody) {
        if (!invokeModelResponseBody.isEmpty()) {
            if (invokeModelResponseBody.startsWith(JSON_START + COMPLETION)) {
                operationType = COMPLETION;
            } else if (invokeModelResponseBody.startsWith(JSON_START + EMBEDDING)) {
                operationType = EMBEDDING;
            } else {
                NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unknown operation type");
            }
        }
    }

    private void setHeaderFields(InvokeModelResponse invokeModelResponse) {
        Map<String, List<String>> headers = invokeModelResponse.sdkHttpResponse().headers();
        try {
            if (!headers.isEmpty()) {
                List<String> inputTokenCountHeaders = headers.get(X_AMZN_BEDROCK_INPUT_TOKEN_COUNT);
                if (inputTokenCountHeaders != null && !inputTokenCountHeaders.isEmpty()) {
                    String result = inputTokenCountHeaders.get(0);
                    inputTokenCount = result != null ? Integer.parseInt(result) : 0;
                }
                List<String> outputTokenCountHeaders = headers.get(X_AMZN_BEDROCK_OUTPUT_TOKEN_COUNT);
                if (outputTokenCountHeaders != null && !outputTokenCountHeaders.isEmpty()) {
                    String result = outputTokenCountHeaders.get(0);
                    outputTokenCount = result != null ? Integer.parseInt(result) : 0;
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
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unable to parse InvokeModelResponse headers");
        }
    }

    public String getCompletion() {
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

    public String getStopReason() {
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

    public String getStop() {
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

    public int getInputTokenCount() {
        return inputTokenCount;
    }

    public int getOutputTokenCount() {
        return outputTokenCount;
    }

    public int getTotalTokenCount() {
        return inputTokenCount + outputTokenCount;
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

    // TODO create errors with below info
    public boolean isErrorResponse() {
        return !isSuccessfulResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getLlmChatCompletionMessageId() {
        return llmChatCompletionMessageId;
    }

    public String getLlmChatCompletionSummaryId() {
        return llmChatCompletionSummaryId;
    }

    public String getLlmEmbeddingId() {
        return llmEmbeddingId;
    }

    public void reportLlmError() {
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("http.statusCode", statusCode);
        errorParams.put("error.code", statusCode);
        if (!llmChatCompletionSummaryId.isEmpty()) {
            errorParams.put("completion_id", llmChatCompletionSummaryId);
        }
        if (!llmEmbeddingId.isEmpty()) {
            errorParams.put("embedding_id", llmEmbeddingId);
        }
        NewRelic.noticeError("LlmError: " + statusText, errorParams);
    }
}
