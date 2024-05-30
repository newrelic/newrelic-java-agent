package com.newrelic.api.agent;

/**
 * An interface for calculating the number of tokens used for a given LLM (Large Language Model) and content.
 * <p>
 * Implement this interface to define custom logic for token calculation based on your application's requirements.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * class MyTokenCountCallback implements LlmTokenCountCallback {
 *
 *     @Override
 *     public int calculateLlmTokenCount(String model, String content) {
 *         // Implement your custom token calculating logic here
 *         // This example calculates the number of tokens based on the length of the content
 *         return content.length();
 *     }
 * }
 *
 * LlmTokenCountCallback myCallback = new MyTokenCountCallback();
 * // After creating the myCallback instance, it should be passed as an argument to the setLlmTokenCountCallback
 * // method of the AI Monitoring API.
 * NewRelic.getAgent().getAiMonitoring.setLlmTokenCountCallback(myCallback);
 * }</pre>
 * </p>
 */
public interface LlmTokenCountCallback {

    /**
     * Calculates the number of tokens used for a given LLM model and content.
     *
     * @param model   The name of the LLM model.
     * @param content The message content or prompt.
     * @return An integer representing the number of tokens used for the given model and content.
     *         If the count cannot be determined or is less than or equal to 0, 0 is returned.
     */
    int calculateLlmTokenCount(String model, String content);
}
