/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReactorNettyHeaders implements Headers {

    private HttpHeaders requestHeaders = null;
    private HttpHeaders responseHeaders = null;

    public ReactorNettyHeaders(HttpClientRequest req) {
        this(req, null);
    }

    public ReactorNettyHeaders(HttpClientResponse resp) {
        this(null, resp);
    }

    public ReactorNettyHeaders(HttpClientRequest req, HttpClientResponse resp) {
        requestHeaders = req == null ? null : req.requestHeaders();
        responseHeaders = resp == null ? null : resp.responseHeaders();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        if (responseHeaders != null && name != null) {
            return responseHeaders.get(name);
        }
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> list = new ArrayList<String>();
        if (responseHeaders != null && name != null) {
            List<String> values = responseHeaders.getAll(name);
            if (values != null) {
                list.addAll(values);
            }
        }
        return list;
    }

    @Override
    public void setHeader(String name, String value) {
        if (requestHeaders != null && name != null && value != null) {
            requestHeaders.set(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (requestHeaders != null && name != null && value != null) {
            requestHeaders.add(name, value);
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        List<String> headerNames = new ArrayList<String>();
        if (requestHeaders != null) {
            headerNames.addAll(requestHeaders.names());
        }
        if (responseHeaders != null) {
            headerNames.addAll(responseHeaders.names());
        }
        return headerNames;
    }

    @Override
    public boolean containsHeader(String name) {
        if (name == null) {
            return false;
        }
        Collection<String> names = getHeaderNames();
        return names.contains(name);
    }
}