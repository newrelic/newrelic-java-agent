/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.vertx.instrumentation;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpServerResponseWrapper extends ExtendedResponse {

    private final RoutingContext context;

    public VertxHttpServerResponseWrapper(RoutingContext context) {
        this.context = context;
    }

    @Override
    public int getStatus() throws Exception {
        return context.response().getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return context.response().getStatusMessage();
    }

    @Override
    public String getContentType() {
        return context.response().headers().get("Content-Type");
    }

    @Override
    public long getContentLength() {
        String contentLength = context.response().headers().get("Content-Length");
        if (contentLength == null) {
            return -1;
        }
        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        context.response().putHeader(name, value);
    }
}
