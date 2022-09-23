package com.sun.net.httpserver;

import java.util.Enumeration;
import java.util.List;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public final class ExchangeRequestResponse extends ExtendedRequest implements Response {

    private final HttpExchange exchange;

    public ExchangeRequestResponse(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public int getStatus() throws Exception {
        return exchange.getResponseCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
        exchange.getResponseHeaders().add(name, value);
    }

    @Override
    public String getContentType() {
        return "text/xml";
    }

    @Override
    public String getRequestURI() {
        return exchange.getRequestURI().getPath();
    }

    @Override
    public String getHeader(String name) {
        List<String> list = exchange.getRequestHeaders().get(name);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
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
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getMethod() {
        return exchange.getRequestMethod();
    }

    @Override
    public List<String> getHeaders(String name) {
        return exchange.getRequestHeaders().get(name);
    }
}
