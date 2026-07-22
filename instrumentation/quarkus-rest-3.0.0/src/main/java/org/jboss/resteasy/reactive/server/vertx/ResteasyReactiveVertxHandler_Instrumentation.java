/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.jboss.resteasy.reactive.server.vertx;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.QuarkusUtils;
import io.vertx.ext.web.RoutingContext;

@Weave(originalName = "org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveVertxHandler", type = MatchType.ExactClass)
public class ResteasyReactiveVertxHandler_Instrumentation {

    // We're going to piggyback off of the transaction started in the vertx-web module
    @Trace(async = true)
    public void handle(RoutingContext event) {
        Token vertxToken = event.get(QuarkusUtils.VERTX_TOKEN_KEY);
        if (vertxToken != null) {
            vertxToken.link();
        }
        Weaver.callOriginal();
    }
}
