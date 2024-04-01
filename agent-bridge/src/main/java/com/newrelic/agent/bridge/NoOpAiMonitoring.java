package com.newrelic.agent.bridge;

import com.newrelic.api.agent.AiMonitoring;
import com.newrelic.api.agent.LlmTokenCountCallback;

import java.util.Map;

public class NoOpAiMonitoring implements AiMonitoring {

    static final AiMonitoring INSTANCE = new NoOpAiMonitoring();

    private NoOpAiMonitoring() {}

    @Override
    public void recordLlmFeedbackEvent(Map<String, Object> llmFeedbackEventAttributes) {
    }

    @Override
    public void setLlmTokenCountCallback(LlmTokenCountCallback llmTokenCountCallback) {
    }
}
