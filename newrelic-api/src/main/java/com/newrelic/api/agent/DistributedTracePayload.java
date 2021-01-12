/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.Map;

/**
 * Payload used to connect two services in a distributed system.
 *
 * Use the Distributed Tracing API {@link Transaction#insertDistributedTraceHeaders(Map)} to create distributed tracing headers
 * and {@link Transaction#acceptDistributedTraceHeaders(HeaderType, Map)} to link the services together.
 */
public interface DistributedTracePayload {

    /**
     * Get the distributed trace payload in JSON String format.
     *
     * @return a JSON String representation of the payload, or empty string if distributed_tracing.exclude_newrelic_header is set
     */
    String text();

    /**
     * Get the distributed trace payload in base64 encoded JSON String format
     *
     * @return a base64 encoded JSON String representation of the payload, or empty string if distributed_tracing.exclude_newrelic_header is set
     */
    String httpSafe();

}
