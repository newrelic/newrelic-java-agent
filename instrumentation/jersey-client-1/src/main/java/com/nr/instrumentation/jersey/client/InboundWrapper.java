/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey.client;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.sun.jersey.api.client.ClientResponse;

import java.util.List;

public class InboundWrapper extends ExtendedInboundHeaders {

    private final ClientResponse response;

    public InboundWrapper(ClientResponse response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return response.getMetadata().getFirst(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return response.getMetadata().get(name);
    }
}
