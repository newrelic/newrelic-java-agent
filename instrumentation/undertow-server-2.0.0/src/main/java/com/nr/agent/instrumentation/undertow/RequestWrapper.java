package com.nr.agent.instrumentation.undertow;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Map;

public class RequestWrapper extends ExtendedRequest {
    private final Map<String, Cookie> cookies;
    private final Map<String, Deque<String>> params;
    private final String requestUri;
    private final HeaderMap headers;
    private final HttpString method;

    public RequestWrapper(HttpServerExchange e) {
        cookies = e.getRequestCookies();
        params = e.getPathParameters();
        requestUri = e.getRequestURI();
        headers = e.getRequestHeaders();
        method = e.getRequestMethod();
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        if (cookies != null) {
            Cookie cookie = cookies.get(name);
            if(cookie != null)
                return cookie.getValue();
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getParameterNames() {
        if (params != null) {
            return Collections.enumeration(params.keySet());
        }

        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        if (params != null) {
            Deque<String> values = params.get(name);
            if (values != null) {
                String[] retValue = new String[values.size()];
                values.toArray(retValue);
                return retValue;
            }
        }

        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return requestUri;
    }

    @Override
    public String getHeader(String name) {
        if (headers != null) {
            return headers.getFirst(name);
        }

        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getMethod() {
        return method.toString();
    }
}
