package com.nr.instrumentation.netty_reactor;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper for reading DT headers from Reactor Netty inbound responses.
 * Uses reflection to avoid referencing HttpClientResponse interface which would add
 * io.netty types to Reference-Classes.
 */
public class InboundHeadersWrapper extends ExtendedInboundHeaders {

    private final Object headers;

    public InboundHeadersWrapper(Object headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        try {
            Method getMethod = headers.getClass().getMethod("get", String.class);
            getMethod.setAccessible(true);
            return (String) getMethod.invoke(headers, name);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getHeaders(String name) {
        try {
            Method getAllMethod = headers.getClass().getMethod("getAll", String.class);
            getAllMethod.setAccessible(true);
            List<String> result = (List<String>) getAllMethod.invoke(headers, name);
            return result != null ? result : Collections.emptyList();
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}