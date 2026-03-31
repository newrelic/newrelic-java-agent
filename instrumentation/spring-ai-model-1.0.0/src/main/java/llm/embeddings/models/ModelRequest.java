/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.models;

public interface ModelRequest {
    /**
     * Get the input to the embedding creation call.
     *
     * @param index int indicating the index of a message in an array. May be ignored for request structures that always return a single message.
     * @return String representing the input to the embedding creation call
     */
    String getInputText(int index);

    /**
     * Get the number of input text messages from the embedding request.
     *
     * @return int representing the number of request messages returned
     */
    int getNumberOfInputTextMessages();

    /**
     * Get the LLM model ID.
     *
     * @return String representing the LLM model ID
     */
    String getModelId();
}
