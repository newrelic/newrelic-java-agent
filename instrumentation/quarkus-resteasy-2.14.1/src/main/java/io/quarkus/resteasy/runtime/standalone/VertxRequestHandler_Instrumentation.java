/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.quarkus.resteasy.runtime.standalone;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.vertx.ext.web.RoutingContext;

import java.io.InputStream;

@Weave(originalName = "io.quarkus.resteasy.runtime.standalone.VertxRequestHandler", type = MatchType.ExactClass)
public class VertxRequestHandler_Instrumentation {

    @Trace(async = true)
    public void handle(RoutingContext request) {
        Token vertxToken = request.get("newrelic-token");
        if (vertxToken != null) {
            vertxToken.link();
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    private void dispatch(RoutingContext routingContext, InputStream is, VertxOutput output) {
        Token vertxToken = routingContext.get("newrelic-token");
        if (vertxToken != null) {
            vertxToken.link();
        }

        // Remove the Vert.x path queue so nameTransaction() returns early instead of
        // overwriting the JAX-RS template name set by jax-rs instrumentation.
        routingContext.data().remove("newrelic-path");
        Weaver.callOriginal();
        NewRelic.getAgent().getTransaction().getTransactionName();
    }
}
