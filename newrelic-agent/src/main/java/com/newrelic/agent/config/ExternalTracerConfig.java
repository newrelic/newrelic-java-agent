/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface ExternalTracerConfig {

    /**
     * @return true if external_tracer.exclude_request_uri is true
     */
    boolean excludeRequestUri();

}
