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
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Http2RequestHeaderWrapper extends ExtendedRequest {
    private static final Pattern URL_REPLACEMENT_PATTERN = Pattern.compile("(?i)%(?![\\da-f]{2})");
    private final Set<Cookie> cookies;
    private final Map<String, List<String>> parameters;
    private final Http2Headers http2Headers;
    private final CharSequence method;
    private final CharSequence path;
    private final CharSequence authority;

    public Http2RequestHeaderWrapper(Http2Headers http2Headers) {
        super();
        this.http2Headers = http2Headers;
        this.method = getMethodHeader();
        this.path = getPathHeader();
        this.authority = getAuthorityHeader();
        this.cookies = getCookies();
        this.parameters = getParameters();
    }

    private Map<String, List<String>> getParameters() {
        Map<String, List<String>> params = null;
        try {
            String uri;
            if (path != null) {
                uri = path.toString();
                // Escape any percent signs in the URI
                uri = URL_REPLACEMENT_PATTERN.matcher(uri).replaceAll("%25");
                QueryStringDecoder decoderQuery = new QueryStringDecoder(uri);
                params = decoderQuery.parameters();
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode URI: {0}", path);
            params = new LinkedHashMap<>();
        }
        return params;
    }

    private Set<Cookie> getCookies() {
        Set<Cookie> rawCookies = null;
        try {
            if (http2Headers.contains(HttpHeaderNames.COOKIE)) {
                CharSequence cookie = http2Headers.get(HttpHeaderNames.COOKIE);
                try {
                    if (cookie != null) {
                        rawCookies = ServerCookieDecoder.STRICT.decode(cookie.toString());
                    }
                } catch (Exception e) {
                    AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode cookie: {0}", cookie);
                    rawCookies = Collections.emptySet();
                }
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers cookies: {0}", e.getMessage());
        }
        return rawCookies;
    }

    private CharSequence getMethodHeader() {
        CharSequence method = null;
        try {
            method = http2Headers.method();
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers method: {0}", e.getMessage());
        }
        return method;
    }

    private CharSequence getPathHeader() {
        CharSequence path = null;
        try {
            path = http2Headers.path();
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers path: {0}", e.getMessage());
        }
        return path;
    }

    private CharSequence getAuthorityHeader() {
        CharSequence authority = null;
        try {
            authority = http2Headers.authority();
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers authority: {0}", e.getMessage());
        }
        return authority;
    }

    @Override
    public String getRequestURI() {
        if (path == null) {
            return null;
        }
        return path.toString();
    }

    @Override
    public String getHeader(String name) {
        try {
            // HTTP/2 only supports lowercase headers
            String lowerCaseHeaderName = name.toLowerCase();
            if (lowerCaseHeaderName.equals(HttpHeaderNames.HOST.toString())) {
                return getHost();
            }

            if (http2Headers.contains(lowerCaseHeaderName)) {
                return http2Headers.get(lowerCaseHeaderName).toString();
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers header: {0}", e.getMessage());
        }
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getParameterNames() {
        if (parameters == null) {
            return null;
        }
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
        if (method == null) {
            return null;
        }
        return method.toString();
    }

    public String getHost() {
        try {
            if (http2Headers.contains(HttpHeaderNames.HOST)) {
                return http2Headers.get(HttpHeaderNames.HOST).toString();
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, "host header is not present in Http2Headers");
        }
        if (authority == null) {
            return null;
        }
        return authority.toString();
    }

    @Override
    public List<String> getHeaders(String name) {
        List<String> headers = new ArrayList<>();
        try {
            // HTTP/2 only supports lowercase headers
            String lowerCaseHeaderName = name.toLowerCase();
            List<CharSequence> allHeaders = http2Headers.getAll(lowerCaseHeaderName);
            for (CharSequence header : allHeaders) {
                headers.add(header.toString());
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get Http2Headers headers: {0}", e.getMessage());
        }
        return headers;
    }
}
