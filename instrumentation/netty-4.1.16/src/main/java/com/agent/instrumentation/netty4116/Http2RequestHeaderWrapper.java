///*
// *
// *  * Copyright 2024 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package com.agent.instrumentation.netty4116;
//
//import com.newrelic.agent.bridge.AgentBridge;
//import com.newrelic.api.agent.ExtendedRequest;
//import com.newrelic.api.agent.HeaderType;
//import com.newrelic.api.agent.weaver.Weaver;
//import io.netty.handler.codec.http.HttpHeaderNames;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.QueryStringDecoder;
//import io.netty.handler.codec.http.cookie.Cookie;
//import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
//import io.netty.handler.codec.http2.Http2Exception;
//import io.netty.handler.codec.http2.Http2Headers;
//import io.netty.handler.codec.http2.Http2HeadersFrame;
//import io.netty.handler.codec.http2.HttpConversionUtil;
//
//import java.util.Collections;
//import java.util.Enumeration;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.logging.Level;
//import java.util.regex.Pattern;
//
//public class Http2RequestHeaderWrapper extends ExtendedRequest {
//    private static final Pattern URL_REPLACEMENT_PATTERN = Pattern.compile("(?i)%(?![\\da-f]{2})");
////    private final HttpRequest request;
//    private final Set<Cookie> cookies;
//    private final Map<String, List<String>> parameters;
//
//    private final Http2Headers http2Headers;
//
//    public Http2RequestHeaderWrapper(Http2Headers http2Headers) {
//        super();
//        // There's no reason to mutate the incoming request but the Http2HeadersFrame could be used for that if need be.
//
//        // Use the HttpRequest for getting info from the request
////        this.request = getHttpRequest(http2HeadersFrame);
//        this.http2Headers = http2Headers;
//
//        String scheme = http2Headers.get(":scheme") != null ? http2Headers.get(":scheme").toString() : null;
//        String method = http2Headers.get(":method") != null ? http2Headers.get(":method").toString() : null;
//        String path = http2Headers.get(":path") != null ? http2Headers.get(":path").toString() : null;
//        String authority = http2Headers.get(":authority") != null ? http2Headers.get(":authority").toString() : null;
//
////        headers.get(":scheme") + "://" + headers.get(":authority") + headers.get(":path")
//
//        // TODO see io.vertx.core.http.impl.headers.Http2HeadersAdaptor
//
//        Set<Cookie> rawCookies = null;
//        if (http2Headers.contains(HttpHeaderNames.COOKIE)) {
//            try {
//                rawCookies = ServerCookieDecoder.STRICT.decode(String.valueOf(http2Headers.get(HttpHeaderNames.COOKIE)));
//            } catch (Exception e) {
//                AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode cookie: {0}",
//                        http2Headers.get(HttpHeaderNames.COOKIE));
//                rawCookies = Collections.emptySet();
//            }
//        }
//        this.cookies = rawCookies;
//
//        Map<String, List<String>> params = new LinkedHashMap<>();
//        try {
//            if (http2Headers.contains(HttpHeaderNames.REFERER)) { // FIXME? is referer right?
//                String uri = String.valueOf(http2Headers.get(HttpHeaderNames.REFERER));
//                uri = URL_REPLACEMENT_PATTERN.matcher(uri).replaceAll("%25"); // Escape any percent signs in the URI
//                QueryStringDecoder decoderQuery = new QueryStringDecoder(uri);
//                params = decoderQuery.parameters();
//            }
//        } catch (Exception e) {
//            AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to decode URI: {0}", http2Headers.path());
////            params = new LinkedHashMap<>();
//        }
//        this.parameters = params;
//    }
//
//    @Override
//    public String getRequestURI() {
//        String referer = "";
//        if (http2Headers.contains(HttpHeaderNames.REFERER)) { // FIXME? is referer right?
//            referer = (String) http2Headers.get(HttpHeaderNames.REFERER);
//        }
//        return referer;
//    }
//
//    @Override
//    public String getHeader(String name) {
//        return String.valueOf(http2Headers.get(name));
//    }
//
//    @Override
//    public String getRemoteUser() {
//        return null;
//    }
//
//    @SuppressWarnings("rawtypes")
//    @Override
//    public Enumeration getParameterNames() {
//        return Collections.enumeration(parameters.keySet());
//    }
//
//    @Override
//    public String[] getParameterValues(String name) {
//        List<String> result = parameters.get(name);
//        return (result == null ? null : result.toArray(new String[0]));
//    }
//
//    @Override
//    public Object getAttribute(String name) {
//        return null;
//    }
//
//    @Override
//    public String getCookieValue(String name) {
//        for (Cookie cookie : cookies) {
//            if (cookie.name().equals(name)) {
//                return cookie.value();
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public HeaderType getHeaderType() {
//        return HeaderType.HTTP;
//    }
//
//    @Override
//    public String getMethod() {
//        return http2Headers.method().toString();
//    }
//
//    @Override
//    public List<String> getHeaders(String name) {
//        return Collections.emptyList();
////        return http2Headers.getAll(name); // FIXME
//    }
//
//    /**
//     * Converts an Http2HeadersFrame into an HttpRequest.
//     * <p>
//     * Note: Mutating the resulting HttpRequest does not
//     * mutate the Http2HeadersFrame used in the conversion.
//     *
//     * @param msg Http2HeadersFrame
//     * @return HttpRequest
//     */
//    public static HttpRequest getHttpRequest(Http2HeadersFrame msg) {
//        HttpRequest httpRequest = null;
//        try {
//            // Setting validateHttpHeaders to false will mean that Netty won't
//            // validate & protect against user-supplied header values that are malicious.
//            httpRequest = HttpConversionUtil.toHttpRequest(msg.stream().id(), msg.headers(), true);
//        } catch (Http2Exception e) {
//            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
//        }
//        return httpRequest;
//    }
//}
