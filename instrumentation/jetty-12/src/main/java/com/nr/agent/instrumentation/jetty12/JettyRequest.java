package com.nr.agent.instrumentation.jetty12;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Fields;

import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class JettyRequest extends ExtendedRequest {

    private final Request request;

    public JettyRequest(Request request) {
        this.request = request;
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        return request.getHeaders().get(name);
    }

    @Override
    public String getRequestURI() {
        return request.getHttpURI().getPath();
    }

    @Override
    public String getRemoteUser() {
        Request.AuthenticationState state = Request.getAuthenticationState(request);
        if (state != null) {
            Principal principle = state.getUserPrincipal();
            if (principle != null) {
                return principle.getName();
            }
        }
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        Fields fields = Request.extractQueryParameters(request);
        if (fields == null) {
            return null;
        }
        return Collections.enumeration(fields.getNames());
    }

    @Override
    public String[] getParameterValues(String name) {
        Fields fields = Request.extractQueryParameters(request);
        if (fields == null) {
            return new String[0];
        }

        List<String> valueList = fields.getValues(name);

        return valueList.toArray(new String[0]);
    }

    @Override
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    @Override
    public String getCookieValue(String name) {
        List<HttpCookie> cookies = Request.getCookies(request);

        for (HttpCookie cookie: cookies) {
            if (cookie.getName() != null && cookie.getName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}