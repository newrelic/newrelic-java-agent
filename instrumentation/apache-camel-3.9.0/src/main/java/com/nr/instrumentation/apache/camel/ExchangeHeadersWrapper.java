/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.apache.camel;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import org.apache.camel.Exchange;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExchangeHeadersWrapper implements Headers {
    private final Map<String, String> headers;

    public ExchangeHeadersWrapper(Exchange exchange) {
        Map<String, String> headers = new HashMap<>();
        for (String header : exchange.getIn().getHeaders().keySet()) {
            String value = exchange.getIn().getHeader(header, String.class);
            if (value != null) {
                headers.put(header, value);
            }
        }
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        String header = headers.get(name);
        return Collections.singletonList(header);
    }

    @Override
    public void setHeader(String name, String value) {
        // No-Op
    }

    @Override
    public void addHeader(String name, String value) {
        // No-Op
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }
}
