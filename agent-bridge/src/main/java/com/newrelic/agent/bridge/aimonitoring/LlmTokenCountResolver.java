/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.aimonitoring;

import com.newrelic.api.agent.LlmTokenCountCallback;


/**
 * Resolves token counts for LLM events.
 * <p>
 * <b>Token Count Resolution Order:</b>
 * <ol>
 *   <li>Response usage metadata - when all three fields are present and valid</li>
 *   <li>Customer callback ({@link com.newrelic.api.agent.LlmTokenCountCallback})</li>
 *   <li>Backend tokenization - when token_count is omitted (null)</li>
 * </ol>
 * <p>
 * This utility class uses static methods and cannot be instantiated.
 *
 * @see com.newrelic.api.agent.LlmTokenCountCallback
 * @see LlmTokenCountCallbackHolder
 */

public class LlmTokenCountResolver {

    private LlmTokenCountResolver() {}

    /**
     * Check if all required usage fields are present and valid.
     * All three fields must be non-null and >= 0 to use response usage data.
     *
     * @param promptTokens number of tokens in the prompt/request
     * @param completionTokens number of tokens in the completion/response
     * @param totalTokens total tokens (sum of prompt and completion)
     * @return true if all three fields are present and valid, false otherwise
     */
    public static boolean hasCompleteUsageData(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return promptTokens != null && promptTokens >= 0
                && completionTokens != null && completionTokens >= 0
                && totalTokens != null && totalTokens >= 0;
    }

    /**
     * Get token count for LlmChatCompletionMessage events.
     * <p>
     * Returns 0 if the summary has complete usage data (signals backend not to tokenize).
     * Otherwise, attempts to use the customer callback if available.
     * Returns null if no callback is registered (backend will tokenize).
     *
     * @param completeSummaryUsage true if the summary event has complete usage data from all three fields
     * @param model the LLM model name
     * @param content the message content or prompt text
     * @return 0 if completeSummaryUsage is true, callback result if available, or null for backend tokenization
     */
    public static Integer getMessageTokenCount(boolean completeSummaryUsage, String model, String content) {
        LlmTokenCountCallback llmTokenCountCallback = LlmTokenCountCallbackHolder.getLlmTokenCountCallback();

        if (completeSummaryUsage) {
            return 0;
        } else if (llmTokenCountCallback != null && content != null && !content.isEmpty()) {
            return llmTokenCountCallback.calculateLlmTokenCount(model, content);
        } else {
            return null;
        }
    }

}
