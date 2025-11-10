/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

/**
 * Represents an internal subtype of a CustomInsightsEvent that is sent to the
 * custom_event_data collector endpoint but potentially subject to different
 * validation rules and agent configuration.
 */
public class LlmCustomInsightsEvent {
    // LLM event types
    private static final String LLM_EMBEDDING = "LlmEmbedding";
    private static final String LLM_CHAT_COMPLETION_SUMMARY = "LlmChatCompletionSummary";
    private static final String LLM_CHAT_COMPLETION_MESSAGE = "LlmChatCompletionMessage";

    /**
     * Determines if a CustomInsightsEvent should be treated as a LlmEvent
     *
     * @param eventType type of the current event
     * @return true if eventType is an LlmEvent, else false
     */
    public static boolean isLlmEvent(String eventType) {
        return eventType.equals(LLM_EMBEDDING) || eventType.equals(LLM_CHAT_COMPLETION_MESSAGE) || eventType.equals(LLM_CHAT_COMPLETION_SUMMARY);
    }
}
