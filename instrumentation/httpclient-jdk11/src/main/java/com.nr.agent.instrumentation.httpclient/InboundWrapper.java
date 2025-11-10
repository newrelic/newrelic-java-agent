package com.nr.agent.instrumentation.httpclient;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;

public class InboundWrapper implements InboundHeaders {

    private final HttpHeaders headers;

    public InboundWrapper(HttpResponse response) {
        this.headers = response == null ? null : response.headers();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return headers.firstValue(name).map(String::toString).orElse(null);
    }
}
