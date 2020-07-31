/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import org.asynchttpclient.HttpResponseHeaders;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;

import java.util.List;

/**
 * Wraps async http client's inbound response headers for CAT.
 */
public class InboundWrapper extends ExtendedInboundHeaders {

    private final HttpResponseHeaders headers;

    public InboundWrapper(HttpResponseHeaders headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return headers.getHeaders().get(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return headers.getHeaders().getAll(name);
    }
}
