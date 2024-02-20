/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;

public class InboundWrapper extends ExtendedInboundHeaders {

    private final Map<String, List<String>> headers;

    public InboundWrapper(HttpURLConnection connection) {
        this.headers = connection == null ? null : connection.getHeaderFields();
    }

    @Override
    public String getHeader(String name) {
        if (headers == null || name == null) return null;

        List<String> result = headers.get(name);
        return result == null || result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<String> getHeaders(String name) {
        if (headers == null  || name == null) return null;
        List<String> result = headers.get(name);

        return result == null || result.isEmpty() ? null : result;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
