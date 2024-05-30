package com.newrelic.agent.bridge.aimonitoring;

import com.newrelic.api.agent.LlmTokenCountCallback;

/**
 * A thread-safe holder for an instance of {@link LlmTokenCountCallback}.
 * This class provides methods for setting and retrieving the callback instance.
 */
public class LlmTokenCountCallbackHolder {

    private static volatile LlmTokenCountCallback llmTokenCountCallback = null;

    /**
     * Sets the {@link LlmTokenCountCallback} instance to be stored.
     *
     * @param newLlmTokenCountCallback the callback instance
     */
    public static void setLlmTokenCountCallback(LlmTokenCountCallback newLlmTokenCountCallback) {
        llmTokenCountCallback = newLlmTokenCountCallback;
    }

    /**
     * Retrieves the stored {@link LlmTokenCountCallback} instance.
     *
     * @return stored callback instance
     */
    public static LlmTokenCountCallback getLlmTokenCountCallback() {
        return llmTokenCountCallback;
    }
}