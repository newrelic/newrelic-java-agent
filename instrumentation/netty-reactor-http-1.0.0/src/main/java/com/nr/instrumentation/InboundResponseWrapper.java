/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import reactor.netty.http.client.HttpClientResponse;

import java.util.List;

public class InboundResponseWrapper extends ExtendedInboundHeaders {

    private final HttpClientResponse response;

    public InboundResponseWrapper(HttpClientResponse response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        List<String> headers = response.responseHeaders().getAll(name);
        if (headers != null && !headers.isEmpty()) {
            return headers.get(0);
        }
        return null;
    }
}