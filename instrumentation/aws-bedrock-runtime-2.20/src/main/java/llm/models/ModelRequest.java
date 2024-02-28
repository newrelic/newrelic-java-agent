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
    int getMaxTokensToSample();

    float getTemperature();

    String getRequestMessage();

    String getRole();

    String getInputText();

    String getModelId();

    static void logParsingFailure(Exception e, String fieldBeingParsed) {
        if (e != null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "AIM: Error parsing " + fieldBeingParsed + " from ModelRequest");
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Unable to parse empty/null " + fieldBeingParsed + " from ModelRequest");
        }
    }
}
