/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.anthropic.claude;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelResponse;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import static llm.models.ModelInvocation.getRandomGuid;
import static llm.models.ModelResponse.logParsingFailure;

/**
 * Stores the required info from the Bedrock InvokeModelResponse without holding
 * a reference to the actual request object to avoid potential memory issues.
 */
public class AnthropicClaudeInvokeModelResponse implements ModelResponse {
    private static final String STOP_REASON = "stop_reason";

    // Response headers
    private static final String X_AMZN_BEDROCK_INPUT_TOKEN_COUNT = "X-Amzn-Bedrock-Input-Token-Count";
    private static final String X_AMZN_BEDROCK_OUTPUT_TOKEN_COUNT = "X-Amzn-Bedrock-Output-Token-Count";
    private static final String X_AMZN_REQUEST_ID = "x-amzn-RequestId";

    private int inputTokenCount = 0;
    private int outputTokenCount = 0;
    private String amznRequestId = "";

    // LLM operation type
    private String operationType = "";

    // HTTP response
    private boolean isSuccessfulResponse = false;
    private int statusCode = 0;
    private String statusText = "";

    private String llmChatCompletionSummaryId = "";
    private String llmEmbeddingId = "";

    private String invokeModelResponseBody = "";
    private Map<String, JsonNode> responseBodyJsonMap = null;

    private static final String JSON_START = "{\"";

    public AnthropicClaudeInvokeModelResponse(InvokeModelResponse invokeModelResponse) {
        if (invokeModelResponse != null) {
            invokeModelResponseBody = invokeModelResponse.body().asUtf8String();
            isSuccessfulResponse = invokeModelResponse.sdkHttpResponse().isSuccessful();
            statusCode = invokeModelResponse.sdkHttpResponse().statusCode();
            Optional<String> statusTextOptional = invokeModelResponse.sdkHttpResponse().statusText();
            statusTextOptional.ifPresent(s -> statusText = s);
            setOperationType(invokeModelResponseBody);
            setHeaderFields(invokeModelResponse);
            llmChatCompletionSummaryId = getRandomGuid();
            llmEmbeddingId = getRandomGuid();
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
            } else {
                logParsingFailure(null, "response body");
            }
        } catch (Exception e) {
            logParsingFailure(e, "response body");
        }
        return responseBodyJsonMap != null ? responseBodyJsonMap : Collections.emptyMap();
    }

    private void setOperationType(String invokeModelResponseBody) {
        try {
            if (!invokeModelResponseBody.isEmpty()) {
                if (invokeModelResponseBody.startsWith(JSON_START + COMPLETION)) {
                    operationType = COMPLETION;
                } else if (invokeModelResponseBody.startsWith(JSON_START + EMBEDDING)) {
                    operationType = EMBEDDING;
                } else {
                    logParsingFailure(null, "operation type");
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, "operation type");
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
                    amznRequestId = amznRequestIdHeaders.get(0);
                }
            } else {
                logParsingFailure(null, "response headers");
            }
        } catch (Exception e) {
            logParsingFailure(e, "response headers");
        }
    }

    /**
     * Represents the response message
     *
     * @return
     */
    @Override
    public String getResponseMessage() {
        String completion = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(COMPLETION);
                if (jsonNode.isString()) {
                    completion = jsonNode.asString();
                }
            } else {
                logParsingFailure(null, COMPLETION);
            }
        } catch (Exception e) {
            logParsingFailure(e, COMPLETION);
        }
        return completion;
    }

    @Override
    public String getStopReason() {
        String stopReason = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(STOP_REASON);
                if (jsonNode.isString()) {
                    stopReason = jsonNode.asString();
                }
            } else {
                logParsingFailure(null, STOP_REASON);
            }
        } catch (Exception e) {
            logParsingFailure(e, STOP_REASON);
        }
        return stopReason;
    }

    @Override
    public int getInputTokenCount() {
        return inputTokenCount;
    }

    @Override
    public int getOutputTokenCount() {
        return outputTokenCount;
    }

    @Override
    public int getTotalTokenCount() {
        return inputTokenCount + outputTokenCount;
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
    public String getLlmEmbeddingId() {
        return llmEmbeddingId;
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
}
