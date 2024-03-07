/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.ai21labs.jurassic;

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
public class JurassicModelResponse implements ModelResponse {
    private static final String FINISH_REASON = "finishReason";
    private static final String REASON = "reason";
    private static final String COMPLETIONS = "completions";
    private static final String DATA = "data";
    private static final String TEXT = "text";

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

    public JurassicModelResponse(InvokeModelResponse invokeModelResponse) {
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

    /**
     * Parses the operation type from the response body and assigns it to a field.
     *
     * @param invokeModelResponseBody response body String
     */
    private void setOperationType(String invokeModelResponseBody) {
        try {
            if (!invokeModelResponseBody.isEmpty()) {
                // Jurassic for Bedrock doesn't support embedding operations
                if (invokeModelResponseBody.contains(COMPLETION)) {
                    operationType = COMPLETION;
                } else {
                    logParsingFailure(null, "operation type");
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, "operation type");
        }
    }

    /**
     * Parses header values from the response object and assigns them to fields.
     *
     * @param invokeModelResponse response object
     */
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

    @Override
    public String getResponseMessage() {
        String parsedResponseMessage = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode completionsJsonNode = getResponseBodyJsonMap().get(COMPLETIONS);
                if (completionsJsonNode.isArray()) {
                    List<JsonNode> jsonNodeArray = completionsJsonNode.asArray();
                    if (!jsonNodeArray.isEmpty()) {
                        JsonNode jsonNode = jsonNodeArray.get(0);
                        if (jsonNode.isObject()) {
                            Map<String, JsonNode> jsonNodeObject = jsonNode.asObject();
                            if (!jsonNodeObject.isEmpty()) {
                                JsonNode dataJsonNode = jsonNodeObject.get(DATA);
                                if (dataJsonNode.isObject()) {
                                    Map<String, JsonNode> dataJsonNodeObject = dataJsonNode.asObject();
                                    if (!dataJsonNodeObject.isEmpty()) {
                                        JsonNode textJsonNode = dataJsonNodeObject.get(TEXT);
                                        if (textJsonNode.isString()) {
                                            parsedResponseMessage = textJsonNode.asString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, TEXT);
        }
        if (parsedResponseMessage.isEmpty()) {
            logParsingFailure(null, TEXT);
        }
        return parsedResponseMessage;
    }

    @Override
    public String getStopReason() {
        String parsedStopReason = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode completionsJsonNode = getResponseBodyJsonMap().get(COMPLETIONS);
                if (completionsJsonNode.isArray()) {
                    List<JsonNode> jsonNodeArray = completionsJsonNode.asArray();
                    if (!jsonNodeArray.isEmpty()) {
                        JsonNode jsonNode = jsonNodeArray.get(0);
                        if (jsonNode.isObject()) {
                            Map<String, JsonNode> jsonNodeObject = jsonNode.asObject();
                            if (!jsonNodeObject.isEmpty()) {
                                JsonNode dataJsonNode = jsonNodeObject.get(FINISH_REASON);
                                if (dataJsonNode.isObject()) {
                                    Map<String, JsonNode> dataJsonNodeObject = dataJsonNode.asObject();
                                    if (!dataJsonNodeObject.isEmpty()) {
                                        JsonNode textJsonNode = dataJsonNodeObject.get(REASON);
                                        if (textJsonNode.isString()) {
                                            parsedStopReason = textJsonNode.asString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, FINISH_REASON);
        }
        if (parsedStopReason.isEmpty()) {
            logParsingFailure(null, FINISH_REASON);
        }
        return parsedStopReason;
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
