/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring_webclient_60;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.List;

/**
 * Wraps Spring 5 Web Client's inbound response headers for CAT.
 */
public class InboundResponseWrapper extends ExtendedInboundHeaders {

    private final ClientResponse response;

    public InboundResponseWrapper(ClientResponse response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        // header(name) will return an empty list if no header values are found
        final List<String> header = response.headers().header(name);
        if (!header.isEmpty()) {
            return header.get(0);
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        return response.headers().header(name);
    }
}
