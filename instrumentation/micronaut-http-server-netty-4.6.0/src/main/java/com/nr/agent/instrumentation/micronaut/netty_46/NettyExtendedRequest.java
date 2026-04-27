package com.nr.agent.instrumentation.micronaut.netty_46;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

import java.util.Enumeration;

public class NettyExtendedRequest extends ExtendedRequest {

    private final HttpRequest request;

    public NettyExtendedRequest(HttpRequest request) {
        this.request = request;
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public String getRequestURI() {
        return request.uri();
    }

    @Override
    public String getRemoteUser() {
        return "";
    }

    @Override
    public Enumeration getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        return "";
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        HttpHeaders headers = request.headers();
        if(headers != null) {
            return  headers.get(name);
        }
        return null;
    }
}
