package com.newrelic.api.agent;

/**
 * A singleton class for holding an instance of {@link LlmTokenCountCallback}.
 * This class ensures that only one instance of the callback is stored and accessed throughout the application.
 */
public class LlmTokenCountCallbackHolder {

    private static volatile LlmTokenCountCallbackHolder INSTANCE;
    private static volatile LlmTokenCountCallback llmTokenCountCallback;

    /**
     * Private constructor to prevent instantiation from outside.
     *
     * @param llmTokenCountCallback The callback method to be stored.
     */
    private LlmTokenCountCallbackHolder(LlmTokenCountCallback llmTokenCountCallback) {
        LlmTokenCountCallbackHolder.llmTokenCountCallback = llmTokenCountCallback;
    };

    /**
     * Returns the singleton instance of the {@code LlmTokenCountCallbackHolder}.
     *
     * @return The singleton instance.
     */
    public static LlmTokenCountCallbackHolder getInstance() {
        if (INSTANCE == null) {
            synchronized (LlmTokenCountCallbackHolder.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LlmTokenCountCallbackHolder(llmTokenCountCallback);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Sets the {@link LlmTokenCountCallback} instance to be stored.
     *
     * @param llmTokenCountCallback The callback instance to be stored.
     */
    public static void setLlmTokenCountCallback(LlmTokenCountCallback llmTokenCountCallback) {
        LlmTokenCountCallbackHolder.llmTokenCountCallback = llmTokenCountCallback;
    }

    /**
     * Retrieves the stored {@link LlmTokenCountCallback} instance.
     *
     * @return The stored callback instance.
     */
    public LlmTokenCountCallback getLlmTokenCountCallback() {
        return llmTokenCountCallback;
    }

}