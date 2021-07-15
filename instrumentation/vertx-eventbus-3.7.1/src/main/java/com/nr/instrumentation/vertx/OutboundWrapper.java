package com.nr.instrumentation.vertx;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import io.vertx.core.MultiMap;

public class OutboundWrapper implements OutboundHeaders {

    private final MultiMap headers;

    public OutboundWrapper(MultiMap headers) {
        this.headers = headers;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.add(name, value);
    }

}