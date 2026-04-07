/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty4116;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.logging.Level;

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
        try {
            // HTTP/2 only supports lowercase headers
            String lowerCaseHeaderName = name.toLowerCase();
            http2Headers.set(lowerCaseHeaderName, value);
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to set Http2Headers header: {0}", e.getMessage());
        }
    }

    @Override
    public int getStatus() {
        try {
            CharSequence status = http2Headers.status();
            if (status == null) {
                return -1;
            }
            try {
                return Integer.parseInt(status.toString());
            } catch (NumberFormatException e) {
                return -1;
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers status: {0}", e.getMessage());
            return -1;
        }
    }

    @Override
    public String getStatusMessage() {
        // status message doesn't seem to be available on HTTP/2 headers
        return null;
    }

    @Override
    public String getContentType() {
        try {
            if (http2Headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
                CharSequence contentType = http2Headers.get(HttpHeaderNames.CONTENT_TYPE);
                if (contentType != null) {
                    return contentType.toString();
                }
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers content-type: {0}", e.getMessage());
        }
        return null;
    }

    @Override
    public long getContentLength() {
        try {
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
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers content-length: {0}", e.getMessage());
        }
        return -1;
    }
}
