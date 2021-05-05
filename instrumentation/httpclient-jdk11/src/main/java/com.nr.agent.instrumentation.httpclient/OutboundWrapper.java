package com.nr.agent.instrumentation.httpclient;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;

import java.net.http.HttpRequest;
import java.util.logging.Level;

public class OutboundWrapper implements OutboundHeaders {

    HttpRequest.Builder delegate;

    public OutboundWrapper(HttpRequest.Builder requestBuilder) {
        this.delegate = requestBuilder;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        try {
            delegate.setHeader(name, value);
        } catch (IllegalArgumentException e) {
            NewRelic.getAgent().getLogger().log(Level.FINER, "Failed to set request property. Cause: {0}. "
                            + "(an IllegalArgumentException can be expected if header name or value are not valid.",
                    e.getMessage());
        }
    }

}
