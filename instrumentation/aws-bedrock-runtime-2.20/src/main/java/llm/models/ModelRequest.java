/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

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
     * Get the role of the requester.
     *
     * @return String representing the role of the requester
     */
    String getRole(); // TODO can this just be deleted?

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

    /**
     * Log when a parsing error occurs.
     *
     * @param e                Exception encountered when parsing the request
     * @param fieldBeingParsed field that was being parsed
     */
    static void logParsingFailure(Exception e, String fieldBeingParsed) {
        if (e != null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "AIM: Error parsing " + fieldBeingParsed + " from ModelRequest");
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Unable to parse empty/null " + fieldBeingParsed + " from ModelRequest");
        }
    }
}
