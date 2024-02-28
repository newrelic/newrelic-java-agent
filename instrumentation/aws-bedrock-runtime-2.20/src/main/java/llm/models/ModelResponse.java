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
    String COMPLETION = "completion";
    String EMBEDDING = "embedding";

    String getResponseMessage();

    String getStopReason();

    int getInputTokenCount();

    int getOutputTokenCount();

    int getTotalTokenCount();

    String getAmznRequestId();

    String getOperationType();

    String getLlmChatCompletionSummaryId();

    String getLlmEmbeddingId();

    boolean isErrorResponse();

    int getStatusCode();

    String getStatusText();

    static void logParsingFailure(Exception e, String fieldBeingParsed) {
        if (e != null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "AIM: Error parsing " + fieldBeingParsed + " from ModelResponse");
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Unable to parse empty/null " + fieldBeingParsed + " from ModelResponse");
        }
    }
}
