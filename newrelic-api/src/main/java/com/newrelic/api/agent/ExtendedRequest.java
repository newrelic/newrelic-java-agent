/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * A {@link com.newrelic.api.agent.Request} that provides more information for reporting to New Relic. API users should
 * extend this class when wrapping their HTTP requests rather than simply implementing
 * {@link com.newrelic.api.agent.Request} to receive additional functionality.
 */
public abstract class ExtendedRequest extends ExtendedInboundHeaders implements Request {

    /**
     * The HTTP method (e.g. POST or GET) for this request.
     *
     * @return HTTP method (e.g. POST or GET) for this request
     */
    public abstract String getMethod();
}
