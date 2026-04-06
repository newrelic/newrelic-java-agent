/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import org.springframework.http.HttpHeaders;

/**
 * Wrapper for Spring's HttpHeaders that implements OutboundHeaders.
 * Used to inject distributed tracing headers into outbound HTTP requests.
 */
public class OutboundHeadersWrapper implements OutboundHeaders {

    private final HttpHeaders headers;

    private OutboundHeadersWrapper(HttpHeaders headers) {
        this.headers = headers;
    }

    public static void addOutboundHeaders(HttpHeaders headers) {
        if (headers == null) {
            return;
        }

        OutboundHeadersWrapper wrapper = new OutboundHeadersWrapper(headers);
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(wrapper);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.set(name, value);
    }
}