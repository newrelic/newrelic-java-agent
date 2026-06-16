/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

@Weave(type = MatchType.Interface, originalName = "io.vertx.core.http.impl.HttpServerConnection")
public abstract class HttpServerConnection_Instrumentation {
    public HttpServerConnection handler(Handler<HttpServerRequest> handler) {
        if (handler != null) {
            handler = VertxUtil.wrapRequestHandler(handler);
        }
        return Weaver.callOriginal();
    }
}
