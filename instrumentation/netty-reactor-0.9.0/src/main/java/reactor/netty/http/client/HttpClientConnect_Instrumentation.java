/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package reactor.netty.http.client;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.netty_reactor.ReactorNettyHttpClientUtil;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.util.context.Context;

import java.net.URI;

/**
 * Instrumentation for Reactor Netty HttpClient.
 * Handles both async context propagation (token linking) and external call reporting.
 */
@Weave(originalName = "reactor.netty.http.client.HttpClientConnect")
final class HttpClientConnect_Instrumentation {

    @Weave(originalName = "reactor.netty.http.client.HttpClientConnect$HttpObserver")
    static final class HttpObserver_Instrumentation {

        @NewField
        public Segment segment;

        @NewField
        public URI requestURI;

        public Context currentContext() {
            return Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void onUncaughtException(Connection connection, Throwable error) {
            Context ctx = currentContext();
            Token token = ctx != null ? ctx.getOrDefault("newrelic-token", null) : null;
            if (token != null && token.isActive()) {
                token.link();
            }
            Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void onStateChange(Connection connection, ConnectionObserver.State newState) {
            // Check if connection is HttpClientOperations by class name (avoid instanceof to prevent Reference-Classes issues)
            String className = connection.getClass().getName();
            if (className.contains("HttpClientOperations")) {
                String state = newState.toString();

                if ("[request_prepared]".equals(state)) {
                    // Link the token from Reactor context to make transaction available on this thread
                    Context ctx = currentContext();
                    Token token = ctx != null ? ctx.getOrDefault("newrelic-token", null) : null;
                    if (token != null && token.isActive()) {
                        token.link();
                    }

                    // Start segment and add DT headers
                    segment = ReactorNettyHttpClientUtil.startSegment();
                    if (segment != null) {
                        try {
                            requestURI = ReactorNettyHttpClientUtil.extractURI(connection);
                            ReactorNettyHttpClientUtil.addOutboundHeaders(connection, segment);
                        } catch (Throwable t) {
                            // don't break the request
                            requestURI = null;
                        }
                    }
                } else if ("[response_received]".equals(state)) {
                    // Report external call with response status
                    if (segment != null && requestURI != null) {
                        try {
                            ReactorNettyHttpClientUtil.reportAsExternal(connection, requestURI, segment); // Step into the util class
                        } catch (Throwable t) {
                            // ensure segment ends even if reporting fails
                        } finally {
                            segment.end();
                            segment = null;
                        }
                    }
                }
            }

            Weaver.callOriginal();
        }
    }
}
