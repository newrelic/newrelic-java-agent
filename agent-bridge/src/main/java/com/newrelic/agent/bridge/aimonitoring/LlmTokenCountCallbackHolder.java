package com.newrelic.agent.bridge.aimonitoring;

import com.newrelic.api.agent.LlmTokenCountCallback;

/**
 * storage for an instance of {@link LlmTokenCountCallback}.
 */
public class LlmTokenCountCallbackHolder {

    private static volatile LlmTokenCountCallback llmTokenCountCallback = null;

    public static void setLlmTokenCountCallback(LlmTokenCountCallback newLlmTokenCountCallback) {
        llmTokenCountCallback = newLlmTokenCountCallback;
    }

    public static LlmTokenCountCallback getLlmTokenCountCallback() {
        return llmTokenCountCallback;
    }
}