/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.List;

/**
 * Wraps async http client's inbound response headers for CAT.
 */
public class InboundWrapper extends ExtendedInboundHeaders {

    private final HttpHeaders headers;

    public InboundWrapper(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        if (headers == null) {
            return null;
        }

        String headerValues = headers.get(name);
        if (headerValues != null && !headerValues.isEmpty()) {
            return headerValues;
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        if (headers == null) {
            return null;
        }
        return headers.getAll(name);
    }

}
