/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.undertow.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "io.undertow.server.HttpHandler")
public abstract class HttpHandler_Instrumentation {
    @Trace(dispatcher=true)
    public void handleRequest(HttpServerExchange exchange) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Undertow", "HttpHandler", getClass().getSimpleName(), "handleRequest");
        Weaver.callOriginal();
    }
}
