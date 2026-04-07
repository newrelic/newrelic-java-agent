/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import org.apache.kafka.common.header.Header;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

public class HeadersWrapper implements Headers {

    private final org.apache.kafka.common.header.Headers delegate;

    public HeadersWrapper(org.apache.kafka.common.header.Headers headers) {
        this.delegate = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        String value = null;
        Iterator<Header> iterator = delegate.headers(name).iterator();
        if (iterator.hasNext()) {
            byte[] bytes = iterator.next().value();
            if (bytes != null) {
                value = new String(bytes);
            }
        }
        return value;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        Collection<String> headers = new ArrayList<>();
        Iterator<Header> iterator = delegate.headers(name).iterator();
        while (iterator.hasNext()) {
            byte[] bytes = iterator.next().value();
            if (bytes != null) {
                headers.add(new String(bytes));
            }
        }
        return headers;
    }

    @Override
    public void setHeader(String name, String value) {
        delegate.remove(name);
        delegate.add(name, value.getBytes());
    }

    @Override
    public void addHeader(String name, String value) {
        delegate.add(name, value.getBytes());
    }

    @Override
    public Collection<String> getHeaderNames() {
        Collection<String> headerNames = new HashSet<>();
        for(Header header : delegate) {
            headerNames.add(header.key());
        }
        return headerNames;
    }

    @Override
    public boolean containsHeader(String name) {
        for(Header header : delegate) {
            if (Objects.equals(name,header.key())) {
                return true;
            }
        }
        return false;
    }
}
