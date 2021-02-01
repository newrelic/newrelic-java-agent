/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.TransportType;

/**
 * Payload used to connect two services in a distributed system.
 *
 * @deprecated Instead, use the Distributed Tracing API {@link Transaction#insertDistributedTraceHeaders(Headers)} to create a
 * distributed tracing payload and {@link Transaction#acceptDistributedTraceHeaders(TransportType, Headers)} to link the services
 * together.
 */
@Deprecated
public interface DistributedTracePayload extends com.newrelic.api.agent.DistributedTracePayload {

    String text();

    String httpSafe();

}
