/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

@Weave(originalName = "io.vertx.core.http.impl.Http1xServerConnection")
public class Http1xServerConnection_Instrumentation {

    private static Handler<HttpServerRequest> requestHandler(HttpHandlers handler) {
        Handler<HttpServerRequest> original = Weaver.callOriginal();
        if (original != null) {
            // Wrap the request handler so we can start the transaction and start tracking child threads
            return VertxUtil.wrapRequestHandler(original);
        }
        return original;
    }

}
