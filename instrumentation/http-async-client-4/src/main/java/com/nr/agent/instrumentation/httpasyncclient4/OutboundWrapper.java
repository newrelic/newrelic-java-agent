/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpasyncclient4;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import org.apache.http.HttpRequest;

/**
 * Wraps Apache http async client's outbound request headers for CAT.
 */
public class OutboundWrapper implements OutboundHeaders {

    private final HttpRequest request;

    public OutboundWrapper(HttpRequest request) {
        this.request = request;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        request.setHeader(name, value);
    }
}
