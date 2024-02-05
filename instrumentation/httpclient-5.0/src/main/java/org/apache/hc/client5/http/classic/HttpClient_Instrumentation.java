/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.hc.client5.http.classic;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient50.InstrumentationUtils;
import com.nr.agent.instrumentation.httpclient50.WrappedResponseHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.apache.hc.client5.http.classic.HttpClient")
public class HttpClient_Instrumentation {

    @Trace(leaf = true)
    public HttpResponse execute(ClassicHttpRequest request) throws IOException {
        InstrumentationUtils.doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        try {
            InstrumentationUtils.processResponse(request.getUri(), response);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return response;
    }

    @Trace(leaf = true)
    public HttpResponse execute(ClassicHttpRequest request, HttpContext context) throws IOException {
        InstrumentationUtils.doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        try {
            InstrumentationUtils.processResponse(request.getUri(), response);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return response;
    }

    @Trace(leaf = true)
    public ClassicHttpResponse execute(HttpHost target, ClassicHttpRequest request) throws IOException {
        InstrumentationUtils.doOutboundCAT(request);
        ClassicHttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        try {
            URI actualURI = getUri(target, request);
            InstrumentationUtils.processResponse(actualURI, response);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return response;
    }

    @Trace(leaf = true)
    public HttpResponse execute(HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException {
        InstrumentationUtils.doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        try {
            URI actualURI = getUri(target, request);
            InstrumentationUtils.processResponse(actualURI, response);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return response;
    }

    @Trace(leaf = true)
    public <T> T execute(ClassicHttpRequest request, HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        try {
            responseHandler = new WrappedResponseHandler<>(request.getUri(), responseHandler);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        InstrumentationUtils.doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    @Trace(leaf = true)
    public <T> T execute(ClassicHttpRequest request, HttpContext context, HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        try {
            responseHandler = new WrappedResponseHandler<>(request.getUri(), responseHandler);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        InstrumentationUtils.doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    @Trace(leaf = true)
    public <T> T execute(HttpHost target, ClassicHttpRequest request, HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        try {
            URI actualURI = getUri(target, request);
            responseHandler = new WrappedResponseHandler<>(actualURI, responseHandler);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        InstrumentationUtils.doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    @Trace(leaf = true)
    public <T> T execute(HttpHost target, ClassicHttpRequest request, HttpContext context,
            HttpClientResponseHandler<? extends T> responseHandler) throws IOException {
        try {
            URI actualURI = getUri(target, request);
            responseHandler = new WrappedResponseHandler<>(actualURI, responseHandler);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        InstrumentationUtils.doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            InstrumentationUtils.handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    private static URI getUri(HttpHost target, HttpRequest request) throws URISyntaxException {
        String path = request.getPath();

        // Prefer pulling the remainder of the host info from the HttpPost instance
        if (target != null) {
            return new URI(target.getSchemeName(), null, target.getHostName(), target.getPort(), path, null, null);
        } else {    // Pull from the HttpRequest object
            URI requestURI = new URI(request.getUri().toString());
            String scheme = request.getScheme();
            int port = requestURI.getPort() != -1 ? requestURI.getPort() :
                    ("http".equals(scheme) ? 80 : 443);
            return new URI(scheme, null, requestURI.getHost(), port, path, null, null);
        }
    }
}
