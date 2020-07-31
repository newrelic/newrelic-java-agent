/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.example.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.external.ExternalMetrics;
import com.newrelic.agent.deps.org.apache.http.Header;
import com.newrelic.agent.deps.org.apache.http.client.ClientProtocolException;
import com.newrelic.agent.deps.org.apache.http.client.methods.CloseableHttpResponse;
import com.newrelic.agent.deps.org.apache.http.client.methods.HttpUriRequest;
import com.newrelic.agent.deps.org.apache.http.client.methods.RequestBuilder;
import com.newrelic.agent.deps.org.apache.http.impl.client.CloseableHttpClient;
import com.newrelic.agent.deps.org.apache.http.impl.client.HttpClientBuilder;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;

public class HttpRequestClass {

    @Trace(dispatcher = true)
    public void performHttp(URI uri) {

        try {
            makeCall(uri);
        } catch (Exception e) {
            // do nothing
        }
    }

    @Trace
    public void makeCall(URI uri) throws ClientProtocolException, IOException {
        HttpUriRequest request = RequestBuilder.get().setUri(uri).build();
        CloseableHttpClient connection = HttpClientBuilder.create().build();
        CloseableHttpResponse response = connection.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        ExternalMetrics.makeExternalComponentTrace(AgentBridge.getAgent().getTransaction(),
                AgentBridge.getAgent().getTracedMethod(), "localhost", "NewRelicApacheHttp", true,
                "http://localhost:8083", "GET");
    }

    @Trace(dispatcher = true)
    public void performCatHttp(URI uri) {

        try {
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, false,
                    "Custom", "com.example.http.HttpRequestClass/performCatHttp");

            makeCATCall(uri);

        } catch (Exception e) {
            // do nothing
        }
    }

    @Trace(dispatcher = true)
    public void performCatHttpNoSetName(URI uri) {

        try {
            makeCATCall(uri);

        } catch (Exception e) {
            // do nothing
        }
    }

    @Trace
    public void makeCATCall(URI uri) throws ClientProtocolException, IOException {
        HttpUriRequest request = RequestBuilder.get().setUri(uri).build();
        AgentBridge.getAgent().getTransaction().getCrossProcessState().processOutboundRequestHeaders(
                new OutboundWrapper(request), AgentBridge.getAgent().getTracedMethod());
        CloseableHttpClient connection = HttpClientBuilder.create().build();
        CloseableHttpResponse response = connection.execute(request);
        ExternalMetrics.makeExternalComponentTrace(AgentBridge.getAgent().getTransaction(),
                AgentBridge.getAgent().getTracedMethod(), "localhost", "NewRelicApacheHttp", true,
                "http://localhost:8083", "GET");
        AgentBridge.getAgent().getTransaction().getCrossProcessState().processInboundResponseHeaders(
                new InboundWrapper(response), AgentBridge.getAgent().getTracedMethod(), "localhost",
                "http://localhost:8083", true);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    class OutboundWrapper implements OutboundHeaders {

        private final HttpUriRequest delegate;

        public OutboundWrapper(HttpUriRequest request) {
            this.delegate = request;
        }

        @Override
        public void setHeader(String name, String value) {
            delegate.addHeader(name, value);
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }

    class InboundWrapper extends ExtendedInboundHeaders {
        private final CloseableHttpResponse requestHeaders;

        public InboundWrapper(CloseableHttpResponse requestHeaders) {
            this.requestHeaders = requestHeaders;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            return requestHeaders.getFirstHeader(name).getValue();
        }

        @Override
        public List<String> getHeaders(String name) {
            Header[] headers = requestHeaders.getHeaders(name);
            if (headers.length > 0) {
                List<String> result = new ArrayList<>(headers.length);
                for (Header header : headers) {
                    result.add(header.getValue());
                }
                return result;
            }
            return null;
        }
    }
}
