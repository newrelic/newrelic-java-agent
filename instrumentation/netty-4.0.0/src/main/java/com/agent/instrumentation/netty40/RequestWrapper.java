/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty40;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class RequestWrapper extends ExtendedRequest {
    private static final Pattern URL_REPLACEMENT_PATTERN = Pattern.compile("(?i)%(?![\\da-f]{2})");
    private final HttpRequest request;
    private final Set<Cookie> cookies;
    private final Map<String, List<String>> parameters;

    public RequestWrapper(DefaultHttpRequest request) {
        super();
        this.request = request;

        Set<Cookie> rawCookies = null;
        if (request.headers().contains(HttpHeaders.Names.COOKIE)) {
            try {
                rawCookies = CookieDecoder.decode(request.headers().get(HttpHeaders.Names.COOKIE));
            } catch (Exception e) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode cookie: {0}",
                        request.headers().get(HttpHeaders.Names.COOKIE));
                rawCookies = Collections.emptySet();
            }
        }
        this.cookies = rawCookies;

        Map<String, List<String>> params;
        try {
            String uri = request.getUri();
            uri = URL_REPLACEMENT_PATTERN.matcher(uri).replaceAll("%25"); // Escape any percent signs in the URI
            QueryStringDecoder decoderQuery = new QueryStringDecoder(uri);
            params = decoderQuery.parameters();
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode URI: {0}", request.getUri());
            params = new LinkedHashMap<>();
        }
        this.parameters = params;
    }

    @Override
    public String getRequestURI() {
        return request.getUri();
    }

    @Override
    public String getHeader(String name) {
        return request.headers().get(name);
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        List<String> result = parameters.get(name);
        return (result == null ? null : result.toArray(new String[0]));
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
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
        return request.getMethod().name();
    }

    @Override
    public List<String> getHeaders(String name) {
        return request.headers().getAll(name);
    }
}
