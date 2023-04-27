/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.hc.client5.http.classic;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.httpclient50.InboundWrapper;
import com.nr.agent.instrumentation.httpclient50.OutboundWrapper;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.apache.hc.client5.http.classic.HttpClient")
public class HttpClient_Instrumentation {

    @NewField
    private static final String LIBRARY = "CommonsHttp";

    @NewField
    private static final String PROCEDURE = "execute";

    @NewField
    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    private static void doOutboundCAT(ClassicHttpRequest request) {
        System.out.println("inside doOutboundCAT");
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
                .status(response.getCode(), response.getReasonPhrase())
                .build());
    }

    @Trace(leaf = true)
    public HttpResponse execute(ClassicHttpRequest request) throws IOException {
        doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        try {
            processResponse(request.getUri(), response);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return response;
    }

    @Trace(leaf = true)
    public HttpResponse execute(ClassicHttpRequest request, HttpContext context) throws IOException {
        doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        try {
            processResponse(request.getUri(), response);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return response;
    }

    @Trace(leaf = true)
    public ClassicHttpResponse execute(HttpHost target, ClassicHttpRequest request) throws IOException {
        doOutboundCAT(request);
        ClassicHttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        try {
            URI actualURI = getUri(target, request);
            processResponse(actualURI, response);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return response;
    }

    @Trace(leaf = true)
    public HttpResponse execute(HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException {
        doOutboundCAT(request);
        HttpResponse response;
        try {
            response = Weaver.callOriginal();
        } catch (Exception e) {
            handleUnknownHost(e);
            throw e;
        }
        try {
            URI actualURI = getUri(target, request);
            processResponse(actualURI, response);
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
    public <T> T execute(ClassicHttpRequest request, HttpContext context, HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        try {
            responseHandler = new WrappedResponseHandler<>(request.getUri(), responseHandler);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
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
    public <T> T execute(HttpHost target, ClassicHttpRequest request, HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        try {
            URI actualURI = getUri(target, request);
            responseHandler = new WrappedResponseHandler<>(actualURI, responseHandler);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
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
    public <T> T execute(HttpHost target, ClassicHttpRequest request, HttpContext context,
            HttpClientResponseHandler<? extends T> responseHandler) throws IOException {
        try {
            URI actualURI = getUri(target, request);
            responseHandler = new WrappedResponseHandler<>(actualURI, responseHandler);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
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
        URI requestURI = new URI(request.getRequestUri());
        String scheme = requestURI.getScheme() == null ? target.getSchemeName() : requestURI.getScheme();
        return new URI(scheme, null, target.getHostName(), target.getPort(), requestURI.getPath(), null, null);
    }

    public static class WrappedResponseHandler<T> implements HttpClientResponseHandler<T> {

        private final URI uri;
        private final HttpClientResponseHandler<T> originalResponseHandler;

        public WrappedResponseHandler(URI uri, HttpClientResponseHandler<T> originalResponseHandler) {
            this.uri = uri;
            this.originalResponseHandler = originalResponseHandler;
        }

        @Override
        public T handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
            try {
                processResponse(uri, response);
            } catch (Throwable t) {
                AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to process response");
            }
            return originalResponseHandler.handleResponse(response);
        }

        private static void processResponse(URI requestURI, ClassicHttpResponse response) {
            InboundWrapper inboundCatWrapper = new InboundWrapper(response);
            Agent agent = NewRelic.getAgent();
            TracedMethod method = agent.getTracedMethod();
            NewRelic.getAgent().getLogger().log(Level.INFO, "JGB agent: "+agent+"; method: "+method);
            NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(requestURI)
                    .procedure(PROCEDURE)
                    .inboundHeaders(inboundCatWrapper)
                    .status(response.getCode(), response.getReasonPhrase())
                    .build());
        }
    }
}
