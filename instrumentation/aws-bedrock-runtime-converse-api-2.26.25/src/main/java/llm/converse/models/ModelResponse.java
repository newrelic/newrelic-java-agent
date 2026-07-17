/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.converse.models;

public interface ModelResponse {
    // Operation types
    String COMPLETION = "completion";

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
     * Indicates whether the message is from a user or AI assistant.
     *
     * @return boolean true if the message is from a user, else false
     */
    boolean isUser();

    /**
     * Get the response organization.
     *
     * @return String representing the response organization
     */
    String getResponseOrganization();

    /**
     * Get the number of tokens in the prompt/request from the LLM response usage metadata.
     *
     * @return Integer representing the prompt token count or null if not provided by the model
     */
    Integer getResponseUsagePromptTokens();

    /**
     * Get the number of tokens in the completion/response from the LLM response usage metadata.
     *
     * @return Integer representing the completion token count or null if not provided by the model
     */
    Integer getResponseUsageCompletionTokens();

    /**
     * Get the total number of tokens (prompt + completion) from the LLM response usage metadata.
     *
     * @return Integer representing the total token count or null if not provided by the model
     */
    Integer getResponseUsageTotalTokens();

    /**
     * Determine whether the response message at the given index represents reasoning/thinking content rather than
     * regular text content.
     *
     * @param index int indicating the index of a message in an array.
     * @return boolean true if the message at the given index is reasoning content
     */
    boolean isReasoningMessage(int index);

    /**
     * Get the reasoning/thinking text for the response message at the given index.
     *
     * @param index int indicating the index of a message in an array.
     * @return String representing the reasoning text, or null if this index isn't a reasoning message or no
     * reasoning text was returned (e.g. redacted)
     */
    String getResponseReasoningContent(int index);

    /**
     * Get the opaque reasoning signature/continuation token for the response message at the given index.
     *
     * @param index int indicating the index of a message in an array.
     * @return String representing the reasoning signature, or null if not present
     */
    String getResponseReasoningSignature(int index);

    /**
     * Determine whether the reasoning content for the response message at the given index was redacted by the
     * provider (encrypted, content not visible).
     *
     * @param index int indicating the index of a message in an array.
     * @return boolean true if the reasoning content at the given index was redacted
     */
    boolean isResponseReasoningRedacted(int index);
}
