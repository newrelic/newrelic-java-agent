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

/**
 * Helper class for netty-reactor-http instrumentation. Holds the per-state @Trace(async=true) work so that
 * async tracers are only created for the two states that do meaningful work (REQUEST_PREPARED, RESPONSE_RECEIVED),
 * not for every state transition fired through the observer's onStateChange method.
 * <p>
 * Follows the same precedent as TokenLinkingSubscriber in the netty-reactor-* / reactor-3.3.0 modules:
 * non-@Weave class with @Trace(async=true, excludeFromTransactionTrace=true) on helper methods called
 * from instrumented code.
 */
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
        // Atomically claim the SegmentData. remove() is the single race-free coordination
        // point — whichever thread (this method or a concurrent fallback cleanup handler)
        // sees a non-null return wins the race and owns the Segment lifecycle. The loser
        // sees null and no-ops, so a Segment can never be ended twice.
        ReactorNettyContext.SegmentData data = ReactorNettyContext.remove(connection);
        if (data == null || data.segment == null) {
            return;
        }

        // We own the Segment. Always end it (no orphan); only report the external call
        // if we have the URI — gate failure (null URI) skips the metric but still cleans up.
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

    /**
     * Defensive cleanup invoked on RESPONSE_COMPLETED/DISCONNECTING/RELEASED states. No @Trace
     * because no meaningful tracer work is done here — just close out any lingering Segment so it
     * stops retaining its parent Transaction.
     */
    public static void cleanupOrphanedSegment(Connection connection) {
        ReactorNettyContext.SegmentData data = ReactorNettyContext.remove(connection);
        if (data != null && data.segment != null) {
            data.segment.end();
        }
    }
}