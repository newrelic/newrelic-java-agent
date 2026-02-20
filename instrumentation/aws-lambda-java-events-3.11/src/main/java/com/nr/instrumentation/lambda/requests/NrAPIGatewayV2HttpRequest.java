package com.nr.instrumentation.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

import java.util.Collections;
import java.util.Enumeration;

public class NrAPIGatewayV2HttpRequest extends ExtendedRequest {
    private APIGatewayV2HTTPEvent event;

    public NrAPIGatewayV2HttpRequest(APIGatewayV2HTTPEvent event) {
        super();
        this.event = event;
    }

    @Override
    public String getRequestURI() {
        return event.getRawPath();
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
        return new String[]{event.getQueryStringParameters().get(name)};
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        for (String cookie: event.getCookies()) {
            int equalsIndx = cookie.indexOf("=");
            if (equalsIndx != -1 && equalsIndx + 1 < cookie.length()) {
                String key = cookie.substring(0, equalsIndx);
                if (name.equals(key)) {
                    return cookie.substring(equalsIndx + 1);
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
        return event.getRequestContext().getHttp().getMethod();
    }
}