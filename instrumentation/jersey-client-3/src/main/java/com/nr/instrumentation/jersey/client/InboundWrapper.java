/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey.client;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.glassfish.jersey.client.ClientResponse;

import java.util.List;

public class InboundWrapper extends ExtendedInboundHeaders {

    private final MultivaluedMap<String, String> headers;

    public InboundWrapper(ClientResponse response) {
        this.headers = response == null ? null : response.getHeaders();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        if (headers == null || name == null) return null;

        List<String> result = headers.get(name);
        return result == null || result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<String> getHeaders(String name) {
        if (headers == null || name == null) return null;

        List<String> result = headers.get(name);
        return result == null || result.isEmpty() ? null : result;
    }
}
