package com.newrelic.api.agent;

import java.util.Map;

/**
 * This interface defines methods for recording LlmFeedbackMessage events and setting a callback for token calculation.
 */
public interface AiMonitoring {
    /**
     * Records an LlmFeedbackMessage event.
     *
     * @param llmFeedbackEventAttributes A map containing the attributes of an LlmFeedbackMessage event. To construct
     *                                   the llmFeedbackEventAttributes map, use
     *                                   {@link LlmFeedbackEventAttributes.Builder}
     *                                   <p>The map must include:</p>
     *                                   <ul>
     *                                   <li>"traceId" (String): Trace ID where the chat completion related to the
     *                                   feedback event occurred</li>
     *                                   <li>"rating" (Integer/String): Rating provided by an end user</li>
     *                                   </ul>
     *                                   <p>Optional attributes:
     *                                   <ul>
     *                                   <li>"category" (String): Category of the feedback as provided by the end user</li>
     *                                   <li>"message" (String): Freeform text feedback from an end user</li>
     *                                   <li>"metadata" (Map&lt;String, String&gt;): Set of key-value pairs to store
     *                                   additional data to submit with the feedback event</li>
     *                                   </ul>
     *
     *
     */
    void recordLlmFeedbackEvent(Map<String, Object> llmFeedbackEventAttributes);

    /**
     * Sets the callback function for calculating LLM tokens.
     *
     * @param llmTokenCountCallback The callback function to be invoked for counting LLM tokens.
     *                              Example usage:
     *                              <pre>{@code
     *                              LlmTokenCountCallback llmTokenCountCallback = new LlmTokenCountCallback() {
     *                                  {@literal @}Override
     *                                  public Integer calculateLlmTokenCount(String model, String content) {
     *                                      // Token calculation based on model and content goes here
     *                                      // Return the calculated token count
     *                                  }
     *                               };
     *
     *                               // Set the created callback instance
     *                               NewRelic.getAgent().getAiMonitoring().setLlmTokenCountCallback(llmTokenCountCallback);
     *                               }</pre>
     */
    void setLlmTokenCountCallback(LlmTokenCountCallback llmTokenCountCallback);

}
