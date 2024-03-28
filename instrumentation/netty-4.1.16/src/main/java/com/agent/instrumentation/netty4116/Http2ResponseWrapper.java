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
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.HttpConversionUtil;

public class Http2ResponseWrapper extends ExtendedResponse {
    private final HttpResponse response;
    private final Http2HeadersFrame http2HeadersFrame;

    public Http2ResponseWrapper(Http2HeadersFrame http2HeadersFrame) {
        // Use the Http2HeadersFrame for mutating the outgoing response
        this.http2HeadersFrame = http2HeadersFrame;
        // Use the HttpResponse for getting info from the response
        this.response = getHttpResponse(http2HeadersFrame);
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        // If we're mutating the outgoing HTTP/2 response it needs
        // to be done on the actual frame that is being streamed.
        http2HeadersFrame.headers().set(name, value);
    }

    @Override
    public int getStatus() throws Exception {
        return response.status().code();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return response.status().reasonPhrase();
    }

    @Override
    public String getContentType() {
        return response.headers().get(HttpHeaderNames.CONTENT_TYPE);
    }

    @Override
    public long getContentLength() {
        String contentLengthHeader = response.headers().get(HttpHeaderNames.CONTENT_LENGTH);
        try {
            return contentLengthHeader != null ? Long.parseLong(contentLengthHeader) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Converts an Http2HeadersFrame into an HttpResponse.
     * <p>
     * Note: Mutating the resulting HttpResponse does not
     * mutate the Http2HeadersFrame used in the conversion.
     *
     * @param frame Http2HeadersFrame
     * @return HttpResponse
     */
    public static HttpResponse getHttpResponse(Http2HeadersFrame frame) {
        HttpResponse httpResponse = null;
        try {
            // Setting validateHttpHeaders to false will mean that Netty won't
            // validate & protect against user-supplied header values that are malicious.
            httpResponse = HttpConversionUtil.toHttpResponse(frame.stream().id(), frame.headers(), true);
        } catch (Http2Exception e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
        return httpResponse;
    }
}
