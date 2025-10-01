/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring_webclient;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.List;

/**
 * Wraps Spring 5 Web Client's inbound response headers for CAT.
 */
public class InboundResponseWrapper extends ExtendedInboundHeaders {

    private final ClientResponse.Headers headers;

    public InboundResponseWrapper(ClientResponse response) {
        this.headers = response.headers();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        // header(name) will return an empty list if no header values are found
        final List<String> header = headers.header(name);
        if (!header.isEmpty()) {
            return header.get(0);
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        return headers.header(name);
    }
}
