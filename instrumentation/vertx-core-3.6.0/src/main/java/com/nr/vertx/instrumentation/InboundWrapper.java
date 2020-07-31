/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import io.vertx.core.http.HttpClientResponse;

import java.util.List;

public class InboundWrapper extends ExtendedInboundHeaders {

    private final HttpClientResponse response;

    public InboundWrapper(HttpClientResponse response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return response.getHeader(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return response.headers().getAll(name);
    }

}
