/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

/**
 * Payload used to connect two services in a distributed system.
 *
 * Use {@link com.newrelic.api.agent.Transaction#createDistributedTracePayload() createDistributedTracePayload()}
 * to create a payload, and {@link com.newrelic.agent.bridge.Transaction#acceptDistributedTracePayload(com.newrelic.api.agent.DistributedTracePayload)} (DistributedTracePayload)} acceptDistributedTracePayload()} to accept the payload on the second service.
 *
 * @deprecated
 *
 * Note: this is an internal API. Consider using {@link com.newrelic.api.agent.DistributedTracePayload}.
 */
public interface DistributedTracePayload extends com.newrelic.api.agent.DistributedTracePayload {

    String text();

    String httpSafe();

}
