/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.context.Context;

import java.net.URI;

public class ReactorNettyHelper {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public static void handleRequestPrepared(Connection connection, Context ctx) {
        Token token = ctx != null ? ctx.getOrDefault("newrelic-token", null) : null;
        if (token != null && token.isActive()) {
            token.link();
        }

        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn == null) return;

        Segment segment = txn.startSegment("ReactorNettyHttpClient.request");
        if (segment == null) return;

        HttpClientRequest request = (HttpClientRequest) connection;
        String httpMethod = null;
        URI requestUri = null;
        try {
            httpMethod = request.method().name();
            String resourceUrl = request.resourceUrl();
            if (resourceUrl != null && !resourceUrl.isEmpty()) {
                requestUri = URI.create(resourceUrl);
            } else {
                String path = request.uri();
                String hostHeader = request.requestHeaders().get("Host");
                boolean isHttps = connection.channel().pipeline().get("ssl") != null;
                String scheme = isHttps ? "https" : "http";
                String host = (hostHeader != null && !hostHeader.isEmpty()) ? hostHeader : "UnknownHost";
                requestUri = URI.create(scheme + "://" + host + path);
            }
        } catch (Throwable throwable) {
            requestUri = URI.create("http://UnknownHost/unknown");
        }

        if (httpMethod == null) {
            httpMethod = "execute";
        }

        segment.addOutboundRequestHeaders(new OutboundRequestWrapper(request));
        ReactorNettyContext.put(connection, new ReactorNettyContext.SegmentData(segment, requestUri, httpMethod));
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public static void handleResponseReceived(Connection connection) {
        // Atomic remove() is the race free coordination point, Segment.end() cannot be called twice
        ReactorNettyContext.SegmentData data = ReactorNettyContext.remove(connection);
        if (data == null || data.segment == null) {
            return;
        }

        if (data.requestUri != null) {
            HttpClientResponse response = (HttpClientResponse) connection;
            String procedure = (data.httpMethod != null && !data.httpMethod.isEmpty())
                    ? data.httpMethod : "execute";

            data.segment.reportAsExternal(HttpParameters
                    .library(ReactorNettyContext.LIBRARY)
                    .uri(data.requestUri)
                    .procedure(procedure)
                    .inboundHeaders(new InboundResponseWrapper(response))
                    .status(response.status().code(), response.status().reasonPhrase())
                    .build());
        }
        data.segment.end();
    }

    public static void cleanupOrphanedSegment(Connection connection) {
        ReactorNettyContext.SegmentData data = ReactorNettyContext.remove(connection);
        if (data != null && data.segment != null) {
            data.segment.end();
        }
    }
}