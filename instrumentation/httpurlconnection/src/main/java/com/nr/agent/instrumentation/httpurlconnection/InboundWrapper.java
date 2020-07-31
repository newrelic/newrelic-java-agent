/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import java.net.HttpURLConnection;
import java.util.List;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;

public class InboundWrapper extends ExtendedInboundHeaders {

    private final HttpURLConnection connection;

    public InboundWrapper(HttpURLConnection connection) {
        this.connection = connection;
    }

    @Override
    public String getHeader(String name) {
        return connection.getHeaderField(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return connection.getHeaderFields().get(name);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
