/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.Map;

/**
 * Used to send custom events to Insights.
 */
public interface Insights {

    /**
     * Sends an Insights event for the current application.
     *
     * @param eventType Must match /^[a-zA-Z0-9:_ ]+$/, be non-null, and less than 256 chars.
     * @param attributes A map of event data. Each key should be a String and each value should be a String, Number, or Boolean.
     *                   For map values that are not String, Number, or Boolean object types the toString value will be used.
     * @since 3.13.0 
     */
    void recordCustomEvent(String eventType, Map<String, ?> attributes);
}
