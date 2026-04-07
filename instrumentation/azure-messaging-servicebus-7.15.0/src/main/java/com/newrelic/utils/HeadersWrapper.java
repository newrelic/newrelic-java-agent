/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class HeadersWrapper implements Headers {

    private final Map<String, Object> delegate;

    public HeadersWrapper(Map<String, Object> headers) {
        this.delegate = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        return delegate.get(name) == null ? null : delegate.get(name).toString();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        Collection<String> headers = new ArrayList<>();
        headers.add(getHeader(name));
        return headers;
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.remove(name);
        delegate.put(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        delegate.put(name, value);
    }

    @Override
    public Collection<String> getHeaderNames() {
        Collection<String> headerNames = new HashSet<>();
        for(String key : delegate.keySet()) {
            headerNames.add(key);
        }
        return headerNames;
    }

    @Override
    public boolean containsHeader(String name) {
        return delegate.containsKey(name);
    }
}