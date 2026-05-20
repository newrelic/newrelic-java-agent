/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.http.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.InboundResponseWrapper;
import com.nr.instrumentation.OutboundRequestWrapper;
import com.nr.instrumentation.ReactorNettyContext;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.util.context.Context;

import java.net.URI;

@Weave(originalName = "reactor.netty.http.client.HttpClientConnect")
final class HttpClientConnect_Instrumentation {

    @Weave(originalName = "reactor.netty.http.client.HttpClientConnect$HttpIOHandlerObserver")
    static final class HttpIOHandlerObserver_Instrumentation {

        public Context currentContext() {
            return Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void onStateChange(Connection connection, ConnectionObserver.State newState) {

                if (HttpClientState.REQUEST_PREPARED.equals(newState) && connection instanceof HttpClientRequest) {

                    Context ctx = currentContext();
                    Token token = ctx != null ? ctx.getOrDefault("newrelic-token", null) : null;

                    if (token != null && token.isActive()) {
                        token.link();
                    }

                    HttpClientRequest request = (HttpClientRequest) connection;
                    Transaction txn = AgentBridge.getAgent().getTransaction(false);

                    if (txn != null) {
                        Segment segment = txn.startSegment("ReactorNettyHttpClient.request");
                        if (segment != null) {
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
                    }
            } else if (HttpClientState.RESPONSE_RECEIVED.equals(newState) && connection instanceof HttpClientResponse) {

                HttpClientResponse response = (HttpClientResponse) connection;
                ReactorNettyContext.SegmentData data = ReactorNettyContext.remove(connection);

                if (data != null && data.segment != null && data.requestUri != null) {
                    String procedure = (data.httpMethod != null && !data.httpMethod.isEmpty())
                            ? data.httpMethod : "execute";

                    data.segment.reportAsExternal(HttpParameters
                            .library(ReactorNettyContext.LIBRARY)
                            .uri(data.requestUri)
                            .procedure(procedure)
                            .inboundHeaders(new InboundResponseWrapper(response))
                            .status(response.status().code(), response.status().reasonPhrase())
                            .build());
                    data.segment.end();
                }
            }

            Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.netty.http.client.HttpClientConnect$HttpObserver")
    static final class HttpObserver_Instrumentation {

        public Context currentContext() {
            return Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void onUncaughtException(Connection connection, Throwable throwable) {
            ReactorNettyContext.SegmentData data = ReactorNettyContext.remove(connection);
            if (data != null && data.segment != null) {
                data.segment.end();
            }
            Weaver.callOriginal();
        }
    }
}