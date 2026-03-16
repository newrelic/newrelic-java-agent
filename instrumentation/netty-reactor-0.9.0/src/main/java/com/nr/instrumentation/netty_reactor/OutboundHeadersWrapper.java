package com.nr.instrumentation.netty_reactor;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

import java.lang.reflect.Method;

/**
 * Wrapper for adding DT headers to Reactor Netty outbound requests.
 * Uses reflection to avoid referencing HttpClientRequest interface which would add
 * io.netty types to Reference-Classes.
 */
public class OutboundHeadersWrapper implements OutboundHeaders {

    private final Object headers;

    public OutboundHeadersWrapper(Object headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        try {
            Method setMethod = headers.getClass().getMethod("set", String.class, Object.class);
            setMethod.setAccessible(true);
            setMethod.invoke(headers, name, value);
        } catch (Throwable t) {
            // Silently fail - don't break the request
        }
    }
}