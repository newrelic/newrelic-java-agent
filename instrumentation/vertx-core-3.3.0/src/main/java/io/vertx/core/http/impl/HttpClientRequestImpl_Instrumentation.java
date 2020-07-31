/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.OutboundWrapper;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

import static com.nr.vertx.instrumentation.VertxCoreUtil.END;
import static com.nr.vertx.instrumentation.VertxCoreUtil.VERTX_CLIENT;

@Weave(originalName = "io.vertx.core.http.impl.HttpClientRequestImpl")
public abstract class HttpClientRequestImpl_Instrumentation extends HttpClientRequestBase_Instrumentation  {

    private final int port = Weaver.callOriginal();
    private final boolean ssl = Weaver.callOriginal();

    public void end(Buffer chunk) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
        }
        Weaver.callOriginal();
    }

    public void end() {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    protected void doHandleResponse(HttpClientResponseImpl resp) {
        if (segment != null) {
            VertxCoreUtil.processResponse(segment, resp, resp.request().host, port, ssl ? "https" : "http");
            final Token token = segment.getTransaction().getToken();
            segment.end();
            token.linkAndExpire();
        }
        Weaver.callOriginal();
    }

    public abstract MultiMap headers();
}
