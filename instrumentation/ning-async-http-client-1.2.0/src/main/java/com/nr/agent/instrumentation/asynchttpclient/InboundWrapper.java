/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;

import java.util.List;
import java.util.Map;

/**
 * Wraps async http client's inbound response headers for CAT.
 */
public class InboundWrapper extends ExtendedInboundHeaders {

    private final Map<String, List<String>> headers;

    public InboundWrapper(Map<String, List<String>> headers) {
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

        List<String> headerValues = headers.get(name);
        if (headerValues != null && !headerValues.isEmpty()) {
            return headerValues.get(0);
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        if (headers == null) {
            return null;
        }
        return headers.get(name);
    }

}
