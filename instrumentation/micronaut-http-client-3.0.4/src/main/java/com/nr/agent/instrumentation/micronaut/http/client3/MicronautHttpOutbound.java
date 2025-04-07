/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.http.client3;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;

public class MicronautHttpOutbound<B> implements OutboundHeaders {

    private HttpRequest<B> request = null;

    public MicronautHttpOutbound(HttpRequest<B> req) {
        request = req;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        if (request != null) {
            if (request instanceof MutableHttpRequest) {
                MutableHttpRequest<B> mutable = (MutableHttpRequest<B>) request;
                mutable.header(name, value);
            }
        }
    }

}
