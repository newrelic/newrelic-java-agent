/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.OutboundWrapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

import java.util.logging.Level;

import static com.nr.vertx.instrumentation.VertxCoreUtil.END;
import static com.nr.vertx.instrumentation.VertxCoreUtil.VERTX_CLIENT;

@Weave(originalName = "io.vertx.core.http.impl.HttpClientRequestImpl")
public abstract class HttpClientRequestImpl_Instrumentation extends HttpClientRequestBase_Instrumentation {

    public Future<Void> end(Buffer chunk) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
        }
        return Weaver.callOriginal();
    }

    public void end(Buffer chunk, Handler<AsyncResult<Void>> handler) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
        }
        Weaver.callOriginal();
    }

    public void end(Handler<AsyncResult<Void>> handler) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
        }
        Weaver.callOriginal();
    }
}
