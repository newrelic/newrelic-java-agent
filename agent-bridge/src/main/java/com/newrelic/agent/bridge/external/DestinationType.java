/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.api.agent.InboundHeaders;

/**
 * Used for specifying destination of a message action. See {@link com.newrelic.agent.bridge.external.ExternalParametersFactory#createForMessageConsumeOperation(String, DestinationType, String, InboundHeaders)}
 *
 * Keep this copy in sync with the api version.
 */
public enum DestinationType {

    /**
     * Queue with specified name.
     */
    NAMED_QUEUE,

    /**
     * Temporary queue.
     */
    TEMP_QUEUE,

    /**
     * Topic with a specified name.
     */
    NAMED_TOPIC,

    /**
     * Temporary topic.
     */
    TEMP_TOPIC;

    public String getTypeName() {
        switch (this) {
            case NAMED_QUEUE:
            case TEMP_QUEUE:
                return "Queue";
            case NAMED_TOPIC:
            case TEMP_TOPIC:
                return "Topic";
        }

        return "Unknown";
    }

    com.newrelic.api.agent.DestinationType toApiDestinationType() {
        switch (this) {
            case NAMED_QUEUE:
                return com.newrelic.api.agent.DestinationType.NAMED_QUEUE;
            case TEMP_QUEUE:
                return com.newrelic.api.agent.DestinationType.TEMP_QUEUE;
            case NAMED_TOPIC:
                return com.newrelic.api.agent.DestinationType.NAMED_TOPIC;
            case TEMP_TOPIC:
                return com.newrelic.api.agent.DestinationType.NAMED_TOPIC;
            default:
                return null;
        }
    }

}
