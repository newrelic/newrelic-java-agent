/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.netty4116;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

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

    public RequestWrapper(HttpRequest request) {
        super();
        this.request = request;

        Set<Cookie> rawCookies = null;
        if (request.headers().contains(HttpHeaderNames.COOKIE)) {
            try {
                rawCookies = ServerCookieDecoder.STRICT.decode(request.headers().get(HttpHeaderNames.COOKIE));
            } catch (Exception e) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode cookie: {0}",
                        request.headers().get(HttpHeaderNames.COOKIE));
                rawCookies = Collections.emptySet();
            }
        }
        this.cookies = rawCookies;

        Map<String, List<String>> params;
        try {
            String uri = request.uri();
            uri = URL_REPLACEMENT_PATTERN.matcher(uri).replaceAll("%25"); // Escape any percent signs in the URI
            QueryStringDecoder decoderQuery = new QueryStringDecoder(uri);
            params = decoderQuery.parameters();
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode URI: {0}", request.uri());
            params = new LinkedHashMap<>();
        }
        this.parameters = params;
    }

    @Override
    public String getRequestURI() {
        return request.uri();
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
            if (cookie.name().equals(name)) {
                return cookie.value();
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
        return request.method().name();
    }

    @Override
    public List<String> getHeaders(String name) {
        return request.headers().getAll(name);
    }
}
