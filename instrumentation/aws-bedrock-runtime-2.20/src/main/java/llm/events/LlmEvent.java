/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.events;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelInvocation;
import llm.models.ModelRequest;
import llm.models.ModelResponse;
import llm.vendor.Vendor;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for building an LlmEvent
 */
public class LlmEvent {
    private final Map<String, Object> eventAttributes;
    private final Map<String, Object> userLlmAttributes;

    // LLM event types
    public static final String LLM_EMBEDDING = "LlmEmbedding";
    public static final String LLM_CHAT_COMPLETION_SUMMARY = "LlmChatCompletionSummary";
    public static final String LLM_CHAT_COMPLETION_MESSAGE = "LlmChatCompletionMessage";

    // Optional LlmEvent attributes
    private final String spanId;
    private final String traceId;
    private final String vendor;
    private final String ingestSource;
    private final String id;
    private final String content;
    private final String role;
    private final Boolean isResponse;
    private final String requestId;
    private final String responseModel;
    private final Integer sequence;
    private final String completionId;
    private final Integer responseNumberOfMessages;
    private final Float duration;
    private final Boolean error;
    private final String input;
    private final Float requestTemperature;
    private final Integer requestMaxTokens;
    private final String requestModel;
    private final Integer responseUsageTotalTokens;
    private final Integer responseUsagePromptTokens;
    private final Integer responseUsageCompletionTokens;
    private final String responseChoicesFinishReason;

    public static class Builder {
        // Required builder parameters
        private final Map<String, Object> userAttributes;
        private final Map<String, String> linkingMetadata;
        private final ModelRequest modelRequest;
        private final ModelResponse modelResponse;

        /*
         * All optional builder attributes are defaulted to null so that they won't be added
         * to the eventAttributes map unless they are explicitly set via the builder
         * methods when constructing an LlmEvent. This allows the builder to create
         * any type of LlmEvent with any combination of attributes solely determined
         * by the builder methods that are called while omitting all unused attributes.
         */

        // Optional builder parameters
        private String spanId = null;
        private String traceId = null;
        private String vendor = null;
        private String ingestSource = null;
        private String id = null;
        private String content = null;
        private String role = null;
        private Boolean isResponse = null;
        private String requestId = null;
        private String responseModel = null;
        private Integer sequence = null;
        private String completionId = null;
        private Integer responseNumberOfMessages = null;
        private Float duration = null;
        private Boolean error = null;
        private String input = null;
        private Float requestTemperature = null;
        private Integer requestMaxTokens = null;
        private String requestModel = null;
        private Integer responseUsageTotalTokens = null;
        private Integer responseUsagePromptTokens = null;
        private Integer responseUsageCompletionTokens = null;
        private String responseChoicesFinishReason = null;

        public Builder(ModelInvocation modelInvocation) {
            userAttributes = modelInvocation.getUserAttributes();
            linkingMetadata = modelInvocation.getLinkingMetadata();
            modelRequest = modelInvocation.getModelRequest();
            modelResponse = modelInvocation.getModelResponse();
        }

        public Builder spanId() {
            spanId = ModelInvocation.getSpanId(linkingMetadata);
            return this;
        }

        public Builder traceId() {
            traceId = ModelInvocation.getTraceId(linkingMetadata);
            return this;
        }

        public Builder vendor() {
            vendor = Vendor.VENDOR;
            return this;
        }

        public Builder ingestSource() {
            ingestSource = Vendor.INGEST_SOURCE;
            return this;
        }

        public Builder id(String modelId) {
            id = modelId;
            return this;
        }

        public Builder content(String message) {
            content = message;
            return this;
        }

        public Builder role(boolean isUser) {
            if (isUser) {
                role = "user";
            } else {
                role = modelRequest.getRole();
            }
            return this;
        }

        public Builder isResponse(boolean isUser) {
            isResponse = !isUser;
            return this;
        }

        public Builder requestId() {
            requestId = modelResponse.getAmznRequestId();
            return this;
        }

        public Builder responseModel() {
            responseModel = modelRequest.getModelId();
            return this;
        }

        public Builder sequence(int eventSequence) {
            sequence = eventSequence;
            return this;
        }

        public Builder completionId() {
            completionId = modelResponse.getLlmChatCompletionSummaryId();
            return this;
        }

        public Builder responseNumberOfMessages(int numberOfMessages) {
            responseNumberOfMessages = numberOfMessages;
            return this;
        }

        public Builder duration(float callDuration) {
            duration = callDuration;
            return this;
        }

        public Builder error() {
            error = modelResponse.isErrorResponse();
            return this;
        }

        public Builder input() {
            input = modelRequest.getInputText();
            return this;
        }

        public Builder requestTemperature() {
            requestTemperature = modelRequest.getTemperature();
            return this;
        }

        public Builder requestMaxTokens() {
            requestMaxTokens = modelRequest.getMaxTokensToSample();
            return this;
        }

        public Builder requestModel() {
            requestModel = modelRequest.getModelId();
            return this;
        }

        public Builder responseUsageTotalTokens() {
            responseUsageTotalTokens = modelResponse.getTotalTokenCount();
            return this;
        }

        public Builder responseUsagePromptTokens() {
            responseUsagePromptTokens = modelResponse.getInputTokenCount();
            return this;
        }

        public Builder responseUsageCompletionTokens() {
            responseUsageCompletionTokens = modelResponse.getOutputTokenCount();
            return this;
        }

        public Builder responseChoicesFinishReason() {
            responseChoicesFinishReason = modelResponse.getStopReason();
            return this;
        }

        public LlmEvent build() {
            return new LlmEvent(this);
        }
    }

    // This populates the LlmEvent attributes map with only the attributes that were explicitly set on the builder.
    private LlmEvent(Builder builder) {
        // Map of custom user attributes with the llm prefix
        userLlmAttributes = getUserLlmAttributes(builder.userAttributes);

        // Map of all LLM event attributes
        eventAttributes = new HashMap<>(userLlmAttributes);

        spanId = builder.spanId;
        if (spanId != null && !spanId.isEmpty()) {
            eventAttributes.put("span_id", spanId);
        }

        traceId = builder.traceId;
        if (traceId != null && !traceId.isEmpty()) {
            eventAttributes.put("trace_id", traceId);
        }

        vendor = builder.vendor;
        if (vendor != null && !vendor.isEmpty()) {
            eventAttributes.put("vendor", vendor);
        }

        ingestSource = builder.ingestSource;
        if (ingestSource != null && !ingestSource.isEmpty()) {
            eventAttributes.put("ingest_source", ingestSource);
        }

        id = builder.id;
        if (id != null && !id.isEmpty()) {
            eventAttributes.put("id", id);
        }

        content = builder.content;
        if (content != null && !content.isEmpty()) {
            eventAttributes.put("content", content);
        }

        role = builder.role;
        if (role != null && !role.isEmpty()) {
            eventAttributes.put("role", role);
        }

        isResponse = builder.isResponse;
        if (isResponse != null) {
            eventAttributes.put("is_response", isResponse);
        }

        requestId = builder.requestId;
        if (requestId != null && !requestId.isEmpty()) {
            eventAttributes.put("request_id", requestId);
        }

        responseModel = builder.responseModel;
        if (responseModel != null && !responseModel.isEmpty()) {
            eventAttributes.put("response.model", responseModel);
        }

        sequence = builder.sequence;
        if (sequence != null && sequence >= 0) {
            eventAttributes.put("sequence", sequence);
        }

        completionId = builder.completionId;
        if (completionId != null && !completionId.isEmpty()) {
            eventAttributes.put("completion_id", completionId);
        }

        responseNumberOfMessages = builder.responseNumberOfMessages;
        if (responseNumberOfMessages != null && responseNumberOfMessages >= 0) {
            eventAttributes.put("response.number_of_messages", responseNumberOfMessages);
        }

        duration = builder.duration;
        if (duration != null && duration >= 0) {
            eventAttributes.put("duration", duration);
        }

        error = builder.error;
        if (error != null && error) {
            eventAttributes.put("error", true);
        }

        input = builder.input;
        if (input != null && !input.isEmpty()) {
            eventAttributes.put("input", input);
        }

        requestTemperature = builder.requestTemperature;
        if (requestTemperature != null && requestTemperature >= 0) {
            eventAttributes.put("request.temperature", requestTemperature);
        }

        requestMaxTokens = builder.requestMaxTokens;
        if (requestMaxTokens != null && requestMaxTokens >= 0) {
            eventAttributes.put("request.max_tokens", requestMaxTokens);
        }

        requestModel = builder.requestModel;
        if (requestModel != null && !requestModel.isEmpty()) {
            eventAttributes.put("request.model", requestModel);
        }

        responseUsageTotalTokens = builder.responseUsageTotalTokens;
        if (responseUsageTotalTokens != null && responseUsageTotalTokens >= 0) {
            eventAttributes.put("response.usage.total_tokens", responseUsageTotalTokens);
        }

        responseUsagePromptTokens = builder.responseUsagePromptTokens;
        if (responseUsagePromptTokens != null && responseUsagePromptTokens >= 0) {
            eventAttributes.put("response.usage.prompt_tokens", responseUsagePromptTokens);
        }

        responseUsageCompletionTokens = builder.responseUsageCompletionTokens;
        if (responseUsageCompletionTokens != null && responseUsageCompletionTokens >= 0) {
            eventAttributes.put("response.usage.completion_tokens", responseUsageCompletionTokens);
        }

        responseChoicesFinishReason = builder.responseChoicesFinishReason;
        if (responseChoicesFinishReason != null && !responseChoicesFinishReason.isEmpty()) {
            eventAttributes.put("response.choices.finish_reason", responseChoicesFinishReason);
        }
    }

    /**
     * Takes a map of all attributes added by the customer via the addCustomParameter API and returns a map
     * containing only custom attributes with a llm. prefix to be added to LlmEvents.
     *
     * @param userAttributes Map of all custom user attributes
     * @return Map of user attributes prefixed with llm.
     */
    private Map<String, Object> getUserLlmAttributes(Map<String, Object> userAttributes) {
        Map<String, Object> userLlmAttributes = new HashMap<>();

        if (userAttributes != null && !userAttributes.isEmpty()) {
            for (Map.Entry<String, Object> entry : userAttributes.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("llm.")) {
                    userLlmAttributes.put(key, entry.getValue());
                }
            }
        }
        return userLlmAttributes;
    }

    /**
     * Record a LlmChatCompletionMessage custom event
     */
    public void recordLlmChatCompletionMessageEvent() {
        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_CHAT_COMPLETION_MESSAGE, eventAttributes);
    }

    /**
     * Record a LlmChatCompletionSummary custom event
     */
    public void recordLlmChatCompletionSummaryEvent() {
        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_CHAT_COMPLETION_SUMMARY, eventAttributes);
    }

    /**
     * Record a LlmEmbedding custom event
     */
    public void recordLlmEmbeddingEvent() {
        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_EMBEDDING, eventAttributes);
    }
}
