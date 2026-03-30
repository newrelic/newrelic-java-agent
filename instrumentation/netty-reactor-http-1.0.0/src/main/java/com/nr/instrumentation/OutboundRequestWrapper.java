/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import reactor.netty.http.client.HttpClientRequest;

public class OutboundRequestWrapper implements OutboundHeaders {

    private final HttpClientRequest request;

    public OutboundRequestWrapper(HttpClientRequest request) {
        this.request = request;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        request.addHeader(name, value);
    }
}