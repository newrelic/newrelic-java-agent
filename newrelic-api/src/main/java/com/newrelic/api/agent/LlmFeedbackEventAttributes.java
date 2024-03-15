package com.newrelic.api.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LlmFeedbackEventAttributes {
    private final String traceId;
    private final Object rating;
    private final String category;
    private final String message;
    private final Map<String, String> metadata;
    private final UUID id;
    private final String ingestSource;

    protected LlmFeedbackEventAttributes(String traceId, Object rating, String category, String message, Map<String, String> metadata, UUID id, String ingestSource) {
        this.traceId = traceId;
        this.rating = rating;
        this.category = category;
        this.message = message;
        this.metadata = metadata;
        this.id = id;
        this.ingestSource = ingestSource;
    }

    public String getTraceId() {
        return traceId;
    }

    public Object getRating() {
        return rating;
    }


    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public UUID getId() {
        return id;
    }

    public String getIngestSource() {
        return ingestSource;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> feedbackParametersMap = new HashMap<>();
        feedbackParametersMap.put("traceId", getTraceId());
        feedbackParametersMap.put("rating", getRating());
        feedbackParametersMap.put("id", getId());
        feedbackParametersMap.put("ingestSource", getIngestSource());
        if (category != null) {
            feedbackParametersMap.put("category", getCategory());
        }
        if (message != null) {
            feedbackParametersMap.put("message", getMessage());
        }
        if (metadata != null) {
            feedbackParametersMap.put("metadata", getMetadata());
        }
        return feedbackParametersMap;
    }

    public static class Builder {
        private final String traceId;
        private final Object rating;
        private String category;
        private String message;
        private Map<String, String> metadata;
        private final UUID id = UUID.randomUUID();

        public Builder(String traceId, Object rating) {
            this.traceId = traceId;
            this.rating = rating;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Map<String, Object> build() {
            return new LlmFeedbackEventAttributes(traceId, rating, category, message, metadata, id, "Java").toMap();

        }
    }
}
