/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty34;

import com.newrelic.api.agent.ExtendedResponse;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class ResponseWrapper extends ExtendedResponse {
    private final DefaultHttpResponse response;

    public ResponseWrapper(DefaultHttpResponse msg) {
        this.response = msg;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatus().getCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return response.getStatus().getReasonPhrase();
    }

    @Override
    public String getContentType() {
        return response.getHeader(HttpHeaders.Names.CONTENT_TYPE);
    }

    @Override
    public long getContentLength() {
        return response.getContentLength();
    }
}
