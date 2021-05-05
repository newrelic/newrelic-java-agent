package com.nr.agent.instrumentation.httpclient;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;

import java.net.http.HttpResponse;
import java.util.function.Function;

public class InboundWrapper implements InboundHeaders {

    HttpResponse delegate;

    public InboundWrapper(HttpResponse response) {
        this.delegate = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return delegate.headers().firstValue(name).map(String::toString).orElse(null);
    }
}
