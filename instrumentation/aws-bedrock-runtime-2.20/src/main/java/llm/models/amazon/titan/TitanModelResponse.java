/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.amazon.titan;

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
public class TitanModelResponse implements ModelResponse {
    private static final String COMPLETION_REASON = "completionReason";
    private static final String RESULTS = "results";
    private static final String OUTPUT_TEXT = "outputText";

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

    public TitanModelResponse(InvokeModelResponse invokeModelResponse) {
        if (invokeModelResponse != null) {
            invokeModelResponseBody = invokeModelResponse.body().asUtf8String();
            isSuccessfulResponse = invokeModelResponse.sdkHttpResponse().isSuccessful();
            statusCode = invokeModelResponse.sdkHttpResponse().statusCode();
            Optional<String> statusTextOptional = invokeModelResponse.sdkHttpResponse().statusText();
            statusTextOptional.ifPresent(s -> statusText = s);
            setOperationType(invokeModelResponseBody);
            amznRequestId = invokeModelResponse.responseMetadata().requestId();
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
                if (invokeModelResponseBody.contains(COMPLETION_REASON)) {
                    operationType = COMPLETION;
                } else if (invokeModelResponseBody.contains(EMBEDDING)) {
                    operationType = EMBEDDING;
                } else {
                    logParsingFailure(null, "operation type");
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, "operation type");
        }
    }

    @Override
    public String getResponseMessage(int index) {
        String parsedResponseMessage = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(RESULTS);
                if (jsonNode.isArray()) {
                    List<JsonNode> resultsJsonNodeArray = jsonNode.asArray();
                    if (!resultsJsonNodeArray.isEmpty()) {
                        JsonNode resultsJsonNode = resultsJsonNodeArray.get(index);
                        if (resultsJsonNode.isObject()) {
                            Map<String, JsonNode> resultsJsonNodeObject = resultsJsonNode.asObject();
                            if (!resultsJsonNodeObject.isEmpty()) {
                                JsonNode outputTextJsonNode = resultsJsonNodeObject.get(OUTPUT_TEXT);
                                if (outputTextJsonNode.isString()) {
                                    parsedResponseMessage = outputTextJsonNode.asString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, OUTPUT_TEXT);
        }
        if (parsedResponseMessage.isEmpty()) {
            logParsingFailure(null, OUTPUT_TEXT);
        }
        return parsedResponseMessage;
    }

    @Override
    public int getNumberOfResponseMessages() {
        int numberOfResponseMessages = 0;
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(RESULTS);
                if (jsonNode.isArray()) {
                    List<JsonNode> resultsJsonNodeArray = jsonNode.asArray();
                    if (!resultsJsonNodeArray.isEmpty()) {
                        numberOfResponseMessages = resultsJsonNodeArray.size();
                    }
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, RESULTS);
        }
        if (numberOfResponseMessages == 0) {
            logParsingFailure(null, RESULTS);
        }
        return numberOfResponseMessages;
    }

    @Override
    public String getStopReason() {
        String parsedStopReason = "";
        try {
            if (!getResponseBodyJsonMap().isEmpty()) {
                JsonNode jsonNode = getResponseBodyJsonMap().get(RESULTS);
                if (jsonNode.isArray()) {
                    List<JsonNode> resultsJsonNodeArray = jsonNode.asArray();
                    if (!resultsJsonNodeArray.isEmpty()) {
                        JsonNode resultsJsonNode = resultsJsonNodeArray.get(0);
                        if (resultsJsonNode.isObject()) {
                            Map<String, JsonNode> resultsJsonNodeObject = resultsJsonNode.asObject();
                            if (!resultsJsonNodeObject.isEmpty()) {
                                JsonNode outputTextJsonNode = resultsJsonNodeObject.get(COMPLETION_REASON);
                                if (outputTextJsonNode.isString()) {
                                    parsedStopReason = outputTextJsonNode.asString();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logParsingFailure(e, COMPLETION_REASON);
        }
        if (parsedStopReason.isEmpty()) {
            logParsingFailure(null, COMPLETION_REASON);
        }
        return parsedStopReason;
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
