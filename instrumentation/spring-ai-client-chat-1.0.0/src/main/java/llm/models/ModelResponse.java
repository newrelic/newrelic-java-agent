/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
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
     * Get the response message, potentially from a specific array index
     * if multiple messages are returned.
     *
     * @param index int indicating the index of a message in an array. May be ignored for response structures that always return a single message.
     * @return String representing the response message
     */
    String getResponseMessage(int index);

    /**
     * Get the number of response messages returned
     *
     * @return int representing the number of response messages returned
     */
    int getNumberOfResponseMessages();

    /**
     * Get the stop reason.
     *
     * @return String representing the stop reason
     */
    String getStopReason();

    /**
     * Get the Request ID.
     *
     * @return String representing the Request ID
     */
    String getRequestId();

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

    /**
     * Get the response model ID.
     *
     * @return String representing the response model ID
     */
    String getModelId();

    /**
     * Get the response organization.
     *
     * @return String representing the response organization
     */
    String getResponseOrganization();

    /**
     * Get the response usage total token count.
     *
     * @return Integer representing the response usage total token count
     */
    Integer getResponseUsageTotalTokens();

    /**
     * Get the response usage prompt token count.
     *
     * @return Integer representing the response usage prompt token count
     */
    Integer getResponseUsagePromptTokens();

    /**
     * Get the response usage completion token count.
     *
     * @return Integer representing the response usage completion token count
     */
    Integer getResponseUsageCompletionTokens();

    /**
     * Get the duration in milliseconds between when the request was issued and the first token was received.
     *
     * @return Integer representing duration in milliseconds
     */
    Integer getTimeToFirstToken();

    /**
     * Indicates whether the message is from a user or AI assistant.
     *
     * @return boolean true if the message is from a user, else false
     */
    boolean isUser();
}
