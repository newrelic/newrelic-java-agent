/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import org.springframework.http.HttpHeaders;

import java.util.List;

/**
 * Wrapper for Spring's HttpHeaders that implements ExtendedInboundHeaders.
 * Used to read distributed tracing headers from inbound HTTP responses.
 */
public class InboundHeadersWrapper extends ExtendedInboundHeaders {

    private final HttpHeaders headers;

    public InboundHeadersWrapper(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return headers.get(name);
    }
}