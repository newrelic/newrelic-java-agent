/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Used for specifying destination of a message action. See {@link com.newrelic.api.agent.MessageConsumeParameters.Builder}
 * and {@link com.newrelic.api.agent.MessageProduceParameters.Builder}.
 *
 * @since 3.36.0
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
    TEMP_TOPIC,

    /**
     * AMQP Exchange
     */
    EXCHANGE;

    /**
     * Get the generalized type name of a queue source or destination.
     *
     * @return the generalized type name of a queue source or destination.
     */
    public String getTypeName() {
        switch (this) {
        case EXCHANGE:
            return "Exchange";
        case NAMED_QUEUE:
        case TEMP_QUEUE:
            return "Queue";
        case NAMED_TOPIC:
        case TEMP_TOPIC:
            return "Topic";
        }

        return "Unknown";
    }

}
