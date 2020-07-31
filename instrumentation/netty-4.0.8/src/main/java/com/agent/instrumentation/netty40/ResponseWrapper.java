/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty40;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;

public class ResponseWrapper extends ExtendedResponse {
    private final HttpResponse response;

    public ResponseWrapper(HttpResponse msg) {
        this.response = msg;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        response.headers().set(name, value);
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatus().code();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return response.getStatus().reasonPhrase();
    }

    @Override
    public String getContentType() {
        return response.headers().get(HttpHeaders.Names.CONTENT_TYPE);
    }

    @Override
    public long getContentLength() {
        String contentLengthHeader = response.headers().get(HttpHeaders.Names.CONTENT_LENGTH);
        try {
            return contentLengthHeader != null ? Long.parseLong(contentLengthHeader) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
