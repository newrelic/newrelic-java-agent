package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

import java.util.Collections;
import java.util.Enumeration;

public class NrAPIGatewayProxyRequest extends ExtendedRequest {
    private APIGatewayProxyRequestEvent event;

    public NrAPIGatewayProxyRequest(APIGatewayProxyRequestEvent event) {
        super();
        this.event = event;
    }

    @Override
    public String getRequestURI() {
        return event.getPath();
    }

    @Override
    public String getHeader(String name) {
        return event.getHeaders().get(name);
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(event.getQueryStringParameters().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return event.getMultiValueQueryStringParameters().get(name).toArray(new String[0]);
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        String cookieHeader = event.getHeaders().get("Cookie");
        if (cookieHeader != null) {
            for (String cookie: cookieHeader.split(";")) {
                int equalsIndx = cookie.indexOf("=");
                if (equalsIndx != -1 && equalsIndx + 1 < cookie.length()) {
                    String key = cookie.substring(0, equalsIndx);
                    if (name.equals(key.trim())) {
                        return cookie.substring(equalsIndx + 1).trim();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getMethod() {
        return event.getHttpMethod();
    }
}
