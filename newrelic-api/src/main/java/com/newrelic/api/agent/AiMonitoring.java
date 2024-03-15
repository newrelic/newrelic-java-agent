package com.newrelic.api.agent;

import java.util.Map;

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
     * Registers a callback function for providing token counts to LLM events.
     *
     * @param callback Callback function for calculating token counts
     */
//    void setLlmTokenCountCallback(LlmTokenCountCallback callback);

}
