package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class NrAPIGatewayProxyResponse extends ExtendedResponse {
    private final APIGatewayProxyResponseEvent response;

    public NrAPIGatewayProxyResponse(APIGatewayProxyResponseEvent msg) {
        this.response = msg;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeaders(Collections.singletonMap(name, value));
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public String getContentType() {
        return response.getHeaders().get("Content-Type");
    }

    @Override
    public long getContentLength() {
        try {
            String contentLengthHeader = response.getHeaders().get("Content-Length");
            return Long.parseLong(contentLengthHeader);
        } catch (NumberFormatException e) {
            return response.getBody().getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
