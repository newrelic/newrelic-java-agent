package com.newrelic.agent;

import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.api.agent.AiMonitoring;
import com.newrelic.api.agent.LlmFeedbackEventAttributes;
import com.newrelic.api.agent.LlmTokenCountCallback;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;

/**
 * A utility class for interacting with the AI Monitoring API to record LlmFeedbackMessage events.
 * This class implements the {@link AiMonitoring} interface and provides methods for feedback event recording
 * and setting callbacks for token calculation.
 */

public class AiMonitoringImpl implements AiMonitoring {
    private static final String SUPPORTABILITY_AI_MONITORING_TOKEN_COUNT_CALLBACK_SET = "Supportability/AiMonitoringTokenCountCallback/set";

    /**
     * Records an LlmFeedbackMessage event.
     *
     * @param llmFeedbackEventAttributes A map containing the attributes of an LlmFeedbackMessage event. To construct
     *                                   the llmFeedbackEventAttributes map, use
     *                                   {@link LlmFeedbackEventAttributes.Builder}
     *                                   <p>Required Attributes:</p>
     *                                   <ul>
     *                                   <li>"traceId" (String): Trace ID where the chat completion related to the
     *                                   feedback event occurred</li>
     *                                   <li>"rating" (Integer/String): Rating provided by an end user</li>
     *                                   </ul>
     *                                   Optional Attributes:
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

    /**
     * Sets the callback for token calculation and reports a supportability metric.
     *
     * @param llmTokenCountCallback The callback instance implementing {@link LlmTokenCountCallback} interface.
     *                               This callback will be used for token calculation.
     * @see LlmTokenCountCallback
     */
    @Override
    public void setLlmTokenCountCallback(LlmTokenCountCallback llmTokenCountCallback) {
        LlmTokenCountCallbackHolder.setLlmTokenCountCallback(llmTokenCountCallback);
        NewRelic.getAgent().getMetricAggregator().incrementCounter(SUPPORTABILITY_AI_MONITORING_TOKEN_COUNT_CALLBACK_SET);
    }
}
