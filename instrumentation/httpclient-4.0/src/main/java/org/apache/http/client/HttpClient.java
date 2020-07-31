/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.http.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient40.InboundWrapper;
import com.nr.agent.instrumentation.httpclient40.OutboundWrapper;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.logging.Level;

@Weave(type = MatchType.Interface)
public abstract class HttpClient {

    @NewField
    private static final String LIBRARY = "CommonsHttp";

    @NewField
    private static final String PROCEDURE = "execute";

    @NewField
    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    private static void doOutboundCAT(HttpRequest request) {
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(request));
    }

    private static void handleUnknownHost(Exception e) {
        if (e instanceof UnknownHostException) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure(PROCEDURE)
                    .build());
        }
    }

    private static void processResponse(URI requestURI, HttpResponse response) {
        InboundWrapper inboundCatWrapper = new InboundWrapper(response);
        NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                .library(LIBRARY)
                .uri(requestURI)
                .procedure(PROCEDURE)
                .inboundHeaders(inboundCatWrapper)
                .build());
    }

    @Trace(leaf = true)
    public HttpResponse execute(HttpUriRequest request) throws Exception {
        doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        processResponse(request.getURI(), response);
        return response;
    }

    @Trace(leaf = true)
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws Exception {
        doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        processResponse(request.getURI(), response);
        return response;
    }

    @Trace(leaf = true)
    public HttpResponse execute(HttpHost target, HttpRequest request) throws Exception {
        doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        URI actualURI = getUri(target, request);
        processResponse(actualURI, response);
        return response;
    }

    @Trace(leaf = true)
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws Exception {
        doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        URI actualURI = getUri(target, request);
        processResponse(actualURI, response);
        return response;
    }

    @Trace(leaf = true)
    public <T, R extends T> T execute(HttpUriRequest request, ResponseHandler<R> responseHandler)
            throws Exception {
        responseHandler = new WrappedResponseHandler<>(request.getURI(), responseHandler);
        doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    @Trace(leaf = true)
    public <T, R extends T> T execute(HttpUriRequest request, ResponseHandler<R> responseHandler, HttpContext context)
            throws Exception {
        responseHandler = new WrappedResponseHandler<>(request.getURI(), responseHandler);
        doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    @Trace(leaf = true)
    public <T, R extends T> T execute(HttpHost target, HttpRequest request, ResponseHandler<R> responseHandler)
            throws Exception {
        URI actualURI = getUri(target, request);
        responseHandler = new WrappedResponseHandler<>(actualURI, responseHandler);
        doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    @Trace(leaf = true)
    public <T, R extends T> T execute(HttpHost target, HttpRequest request, ResponseHandler<R> responseHandler,
            HttpContext context) throws Exception {
        URI actualURI = getUri(target, request);
        responseHandler = new WrappedResponseHandler<>(actualURI, responseHandler);
        doOutboundCAT(request);
        T response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        return response;
    }

    private static URI getUri(HttpHost target, HttpRequest request) throws URISyntaxException {
        URI requestURI = new URI(request.getRequestLine().getUri());
        String scheme = requestURI.getScheme() == null ? target.getSchemeName() : requestURI.getScheme();
        return new URI(scheme, null, target.getHostName(), target.getPort(), requestURI.getPath(), null, null);
    }

    public static class WrappedResponseHandler<T> implements ResponseHandler<T> {

        private final URI uri;
        private final ResponseHandler<T> originalResponseHandler;

        public WrappedResponseHandler(URI uri, ResponseHandler<T> originalResponseHandler) {
            this.uri = uri;
            this.originalResponseHandler = originalResponseHandler;
        }

        @Override
        public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            try {
                processResponse(uri, response);
            } catch (Throwable t) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to process response");
            }
            return originalResponseHandler.handleResponse(response);
        }

        private static void processResponse(URI requestURI, HttpResponse response) {
            InboundWrapper inboundCatWrapper = new InboundWrapper(response);
            NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(requestURI)
                    .procedure(PROCEDURE)
                    .inboundHeaders(inboundCatWrapper)
                    .build());
        }
    }
}
