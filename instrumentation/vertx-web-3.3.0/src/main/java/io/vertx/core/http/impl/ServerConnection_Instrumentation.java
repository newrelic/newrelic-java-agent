/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.vertx.core.http.impl.ServerConnection")
abstract class ServerConnection_Instrumentation {

    @Trace(dispatcher = true)
    private void handleRequest(HttpServerRequestImpl req, HttpServerResponseImpl resp) {
        Weaver.callOriginal();
    }
}
