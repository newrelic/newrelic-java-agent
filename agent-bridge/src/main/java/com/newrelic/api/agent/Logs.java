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

    /**
     * Sends a LogEvent for the current application, with the linking metadata already fetched. This means
     * that the log event did occur within a transaction, so no need to do that check in the method.
     * This is used for log events generated via async loggers, where we lose the transaction context
     * when the thread hop occurs in the logger.
     *
     * @param attributes A map of log event data (e.g. log message, log timestamp, log level)
     *                   Each key should be a String and each value should be a String, Number, or Boolean.
     *                   For map values that are not String, Number, or Boolean object types the toString value will be used.
     * @param linkingMetadata The previously fetched linking metadata and transaction reference container
     */
    void recordLogEvent(Map<LogAttributeKey, ?> attributes, LinkingMetadataHolder linkingMetadata);
}
