package com.nr.agent.instrumentation.micronaut.netty_46;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NettyHeaders implements Headers {

    private HttpRequest request;

    public NettyHeaders(HttpRequest request) {
        this.request = request;
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

    @Override
    public Collection<String> getHeaders(String name) {
        Set<String> selectedHeaders = new HashSet<>();
        String header = getHeader(name);
        if(header != null) {
            selectedHeaders.add(header);
        }
        return selectedHeaders;
    }

    @Override
    public void setHeader(String name, String value) {

    }

    @Override
    public void addHeader(String name, String value) {

    }

    @Override
    public Collection<String> getHeaderNames() {
        HttpHeaders headers = request.headers();
        if(headers != null) {
            return headers.names();
        }
        return new HashSet<>();
    }

    @Override
    public boolean containsHeader(String name) {
        return getHeaderNames().contains(name);
    }
}
