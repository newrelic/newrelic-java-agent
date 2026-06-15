/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.models;

public interface ModelResponse {
    // Operation type
    String EMBEDDING = "embedding";

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
}
