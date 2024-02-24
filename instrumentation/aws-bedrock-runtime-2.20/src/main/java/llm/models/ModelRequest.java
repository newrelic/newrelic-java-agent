/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

public interface ModelRequest {
    int getMaxTokensToSample();

    float getTemperature();

    String getRequestMessage();

    String getRole();

    String getInputText();

    String getModelId();
}
