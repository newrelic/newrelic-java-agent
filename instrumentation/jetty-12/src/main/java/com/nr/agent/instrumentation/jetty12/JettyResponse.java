package com.nr.agent.instrumentation.jetty12;

import com.newrelic.api.agent.HeaderType;
import org.eclipse.jetty.server.Response;

public class JettyResponse implements com.newrelic.api.agent.Response {

    private final Response response;

    public JettyResponse(Response response) {
        this.response = response;
    }


    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        response.getHeaders().add(name, value);
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public String getContentType() {
        return response.getHeaders().get("content-type");
    }
}
