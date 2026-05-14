/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.completions.models;

public interface ModelRequest {
    /**
     * Get the max tokens allowed for the request.
     *
     * @return int representing the max tokens allowed for the request
     */
    int getMaxTokensToSample();

    /**
     * Get the temperature of the request.
     *
     * @return float representing the temperature of the request
     */
    float getTemperature();

    /**
     * Get the content of the request message, potentially from a specific array index
     * if multiple messages are returned.
     *
     * @param index int indicating the index of a message in an array. May be ignored for request structures that always return a single message.
     * @return String representing the content of the request message
     */
    String getRequestMessage(int index);

    /**
     * Get the number of request messages returned
     *
     * @return int representing the number of request messages returned
     */
    int getNumberOfRequestMessages();

    /**
     * Get the LLM model ID.
     *
     * @return String representing the LLM model ID
     */
    String getModelId();

    /**
     * Indicates whether the message is from a user or AI assistant.
     *
     * @return boolean true if the message is from a user, else false
     */
    boolean isUser();
}
