package com.nr.agent.instrumentation.undertow;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class ResponseWrapper extends ExtendedResponse {
    private final long contentLength;
    private final HeaderMap headers;
    private final int statusCode;

    public ResponseWrapper(HttpServerExchange exchange) {
        contentLength = exchange.getResponseContentLength();
        headers = exchange.getResponseHeaders();
        statusCode = exchange.getStatusCode();
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        if (name != null && value != null) {
            headers.add(new HttpString(name), value);
        }
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    @Override
    public String getStatusMessage() {
        // Not available in the Undertow exchange object
        return null;
    }

    @Override
    public String getContentType() {
        if (headers.contains(Headers.CONTENT_TYPE)) {
            return headers.get(Headers.CONTENT_TYPE).get(0);
        }

        return null;
    }
}
