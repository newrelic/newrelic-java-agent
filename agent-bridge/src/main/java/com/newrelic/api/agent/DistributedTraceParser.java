/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * An interface used by the newrelic opentracing api to parse a payload using the agent (without needing to depend on all agent code)
 */
public interface DistributedTraceParser {

    /**
     * Parse an inbound (String) payload and return an object representing the values
     *
     * @param payload the payload to parse
     * @return the parsed payload object or null if parsing failed
     */
    DistributedTracePayload parseDistributedTracePayload(String payload);

}
