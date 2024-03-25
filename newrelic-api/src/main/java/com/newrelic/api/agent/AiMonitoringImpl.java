package com.newrelic.agent.aimonitoring;

import com.newrelic.api.agent.AiMonitoring;
import com.newrelic.api.agent.LlmFeedbackEventAttributes;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;

/**
 * A utility class for recording LlmFeedbackMessage events using the AI Monitoring API.
 * <p>
 * This class implements the {@link AiMonitoring} interface and provides a method to record LlmFeedbackMessage events
 * by delegating to the Insights API for custom event recording.
 */

public class AiMonitoringImpl implements AiMonitoring {
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
     *                                   Optional attributes:
     *                                   <ul>
     *                                   <li>"category" (String): Category of the feedback as provided by the end user</li>
     *                                   <li>"message" (String): Freeform text feedback from an end user.</li>
     *                                   <li>"metadata" (Map&lt;String, String&gt;): Set of key-value pairs to store
     *                                   additional data to submit with the feedback event</li>
     *                                   </ul>
     */

    @Override
    public void recordLlmFeedbackEvent(Map<String, Object> llmFeedbackEventAttributes) {
        // Delegate to Insights API for event recording
        NewRelic.getAgent().getInsights().recordCustomEvent("LlmFeedbackMessage", llmFeedbackEventAttributes);
    }

    @Override
    public void setLlmTokenCountCallback(LlmTokenCountCallback llmTokenCountCallback) {
        String model = "SampleModel";
        String content = "SampleContent";
//        LlmTokenCountCallbackHolder llmTokenCountCallbackHolder = new LlmTokenCountCallbackHolder(llmTokenCountCallback);
        LlmTokenCountCallbackHolder llmTokenCountCallbackHolder = new LlmTokenCountCallbackHolder();
        llmTokenCountCallbackHolder.setLlmTokenCountCallbackHolder(llmTokenCountCallback);
        LlmTokenCountCallback tokenCounter = llmTokenCountCallbackHolder.getLlmTokenCountCallback();
        Integer tokenCount = llmTokenCountCallback.calculateLlmTokenCount(model, content);

    }
}
