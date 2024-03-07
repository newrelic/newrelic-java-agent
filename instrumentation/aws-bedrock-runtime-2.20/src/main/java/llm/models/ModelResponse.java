/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

public interface ModelResponse {
    // Operation types
    String COMPLETION = "completion";
    String EMBEDDING = "embedding";

    /**
     * Get the response message.
     *
     * @return String representing the response message
     */
    String getResponseMessage();

    /**
     * Get the stop reason.
     *
     * @return String representing the stop reason
     */
    String getStopReason();

    /**
     * Get the Amazon Request ID.
     *
     * @return String representing the Amazon Request ID
     */
    String getAmznRequestId();

    /**
     * Get the operation type.
     *
     * @return String representing the operation type
     */
    String getOperationType();

    /**
     * Get the ID for the associated LlmChatCompletionSummary event.
     *
     * @return String representing the ID for the associated LlmChatCompletionSummary event
     */
    String getLlmChatCompletionSummaryId();

    /**
     * Get the ID for the associated LlmEmbedding event.
     *
     * @return String representing the ID for the associated LlmEmbedding event
     */
    String getLlmEmbeddingId();

    /**
     * Determine whether the response resulted in an error or not.
     *
     * @return boolean true when the LLM response is an error, false when the response was successful
     */
    boolean isErrorResponse();

    /**
     * Get the response status code.
     *
     * @return int representing the response status code
     */
    int getStatusCode();

    /**
     * Get the response status text.
     *
     * @return String representing the response status text
     */
    String getStatusText();

    /**
     * Log when a parsing error occurs.
     *
     * @param e                Exception encountered when parsing the response
     * @param fieldBeingParsed field that was being parsed
     */
    static void logParsingFailure(Exception e, String fieldBeingParsed) {
        if (e != null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "AIM: Error parsing " + fieldBeingParsed + " from ModelResponse");
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Unable to parse empty/null " + fieldBeingParsed + " from ModelResponse");
        }
    }
}
