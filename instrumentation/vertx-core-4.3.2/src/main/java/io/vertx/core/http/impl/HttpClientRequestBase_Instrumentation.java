/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClientResponse;

import java.net.UnknownHostException;
import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "io.vertx.core.http.impl.HttpClientRequestBase")
public abstract class HttpClientRequestBase_Instrumentation {

    @NewField
    public Segment segment;

    public abstract MultiMap headers();

//    @Trace(async = true)
//    public HttpClientRequest response(Handler<AsyncResult<HttpClientResponse>> handler) {
//        AgentBridge.getAgent().getLogger().log(Level.INFO, "vertx4 in HttpClientRequest response  " + handler);
//        if (AgentBridge.getAgent().getTransaction(false) != null) {
//            this.token = NewRelic.getAgent().getTransaction().getToken();
//            System.out.println("agent txn: " + NewRelic.getAgent().getTransaction());
//            AgentBridge.getAgent().getLogger().log(Level.INFO, "vertx4 in HttpClientRequest response");
//            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
//            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
//        }
//
//        return Weaver.callOriginal();
//    }

    @Trace(async = true)
    void handleResponse(Promise<HttpClientResponse> promise, HttpClientResponse resp, long timeoutMs) {
        AgentBridge.getAgent().getLogger().log(Level.INFO, "vertx4 in handleResponse()");
        if (segment != null) {
            AgentBridge.getAgent().getLogger().log(Level.INFO, "vertx4 in handleResponse() segment != null && this.token != null");

            final Token segmentToken = segment.getTransaction().getToken();
            reportExternal(resp, segment);
            segment.end();
            segmentToken.linkAndExpire();

            AgentBridge.getAgent().getTransaction(false).expireAllTokens();
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    void handleException(Throwable t) {
        AgentBridge.getAgent().getLogger().log(Level.INFO, "vertx4 in handleException()");
        if (segment != null) {
            if (t instanceof UnknownHostException) {
                VertxCoreUtil.reportUnknownHost(segment);
            }
            final Token token = segment.getTransaction().getToken();
            segment.end();
            token.linkAndExpire();

            AgentBridge.getAgent().getTransaction(false).expireAllTokens();
        }
        Weaver.callOriginal();
    }

    private void reportExternal(HttpClientResponse response, Segment segment) {
        if (response instanceof HttpClientResponseImpl) {
            HttpClientResponseImpl resp = (HttpClientResponseImpl) response;
            final String host = resp.request().getHost();
            final int port = resp.request().getPort();
            final String scheme = resp.request().ssl ? "https" : "http";
            VertxCoreUtil.processResponse(segment, resp, host, port, scheme);
        }
    }
}
