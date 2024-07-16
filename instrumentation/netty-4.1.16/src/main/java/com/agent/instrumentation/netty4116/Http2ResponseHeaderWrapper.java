/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty4116;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2Headers;

public class Http2ResponseHeaderWrapper extends ExtendedResponse {
    private final Http2Headers http2Headers;

    public Http2ResponseHeaderWrapper(Http2Headers http2Headers) {
        this.http2Headers = http2Headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        // HTTP/2 only supports lowercase headers
        String lowerCaseName = name.toLowerCase();
        try {
            http2Headers.set(lowerCaseName, value);
        } catch (Exception ignored) {
        }
    }

    @Override
    public int getStatus() {
        CharSequence status = http2Headers.status();
        if (status == null) {
            return -1;
        }
        try {
            return Integer.parseInt(status.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String getStatusMessage() throws Exception {
        // FIXME can we get status message????
        CharSequence status = http2Headers.status();
        if (status == null) {
            return null;
        }
        return status.toString();
    }

    @Override
    public String getContentType() {
        if (http2Headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
            CharSequence contentType = http2Headers.get(HttpHeaderNames.CONTENT_TYPE);
            if (contentType != null) {
                return contentType.toString();
            }
        }
        return null;
    }

    @Override
    public long getContentLength() {
        if (http2Headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
            CharSequence contentLength = http2Headers.get(HttpHeaderNames.CONTENT_LENGTH);
            if (contentLength != null) {
                try {
                    return Long.parseLong(contentLength.toString());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }
}
