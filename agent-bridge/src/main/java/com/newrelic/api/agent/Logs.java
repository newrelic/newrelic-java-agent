/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import com.newrelic.agent.bridge.logging.LinkingMetadataHolder;
import com.newrelic.agent.bridge.logging.LogAttributeKey;

import java.util.Map;

/**
 * Used to send LogEvents to New Relic. Each LogEvent represents a single log line.
 */
public interface Logs {

    /**
     * Sends a LogEvent for the current application.
     *
     * @param attributes A map of log event data (e.g. log message, log timestamp, log level)
     *                   Each key should be a String and each value should be a String, Number, or Boolean.
     *                   For map values that are not String, Number, or Boolean object types the toString value will be used.
     * @since 7.6.0
     */
    void recordLogEvent(Map<LogAttributeKey, ?> attributes);
}
