package com.newrelic.instrumentation.labs.ktor.client;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import io.ktor.client.request.HttpRequestBuilder;
import io.ktor.http.HeadersBuilder;

import java.util.Collection;

public class KtorRequestHeaders implements Headers {

    private HttpRequestBuilder requestBuilder;

    public KtorRequestHeaders(HttpRequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        HeadersBuilder headerBuilder = requestBuilder.getHeaders();
        return headerBuilder.get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        HeadersBuilder headerBuilder = requestBuilder.getHeaders();

        return headerBuilder.getAll(name);
    }

    @Override
    public void setHeader(String name, String value) {
        HeadersBuilder headerBuilder = requestBuilder.getHeaders();
        headerBuilder.set(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        HeadersBuilder headerBuilder = requestBuilder.getHeaders();
        headerBuilder.append(name, value);

    }

    @Override
    public Collection<String> getHeaderNames() {
        HeadersBuilder headerBuilder = requestBuilder.getHeaders();
        return headerBuilder.names();
    }

    @Override
    public boolean containsHeader(String name) {
        return getHeaderNames().contains(name);
    }
}
