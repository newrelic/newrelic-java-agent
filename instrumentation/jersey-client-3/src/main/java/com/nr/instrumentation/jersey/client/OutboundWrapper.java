/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey.client;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import org.glassfish.jersey.client.ClientRequest;

import jakarta.ws.rs.core.MultivaluedMap;

public class OutboundWrapper implements OutboundHeaders {

    private final ClientRequest request;

    public OutboundWrapper(ClientRequest request) {
        this.request = request;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        MultivaluedMap<String, Object> headers = request.getHeaders();
        headers.add(name, value);
    }

}