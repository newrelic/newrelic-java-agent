/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.HeaderType;

import java.util.Map;

/**
 * Payload used to connect two services in a distributed system.
 *
 * @deprecated Instead, use the Distributed Tracing API {@link Transaction#insertDistributedTraceHeaders(Map)} to create distributed tracing headers
 * and {@link Transaction#acceptDistributedTraceHeaders(HeaderType, Map)} to link the services together.
 *
 * Note: this is an internal API. Consider using {@link com.newrelic.api.agent.DistributedTracePayload}.
 */
@Deprecated
public interface DistributedTracePayload extends com.newrelic.api.agent.DistributedTracePayload {

    String text();

    String httpSafe();

}
