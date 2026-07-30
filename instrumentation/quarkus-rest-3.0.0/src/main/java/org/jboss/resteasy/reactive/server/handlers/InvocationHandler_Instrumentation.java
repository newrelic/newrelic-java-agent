/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.jboss.resteasy.reactive.server.handlers;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.QuarkusUtils;
import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

@Weave(originalName = "org.jboss.resteasy.reactive.server.handlers.InvocationHandler")
public class InvocationHandler_Instrumentation {

    @Trace(async = true)
    public void handle(ResteasyReactiveRequestContext requestContext) {
        RoutingContext routingContext = requestContext.serverRequest().unwrap(RoutingContext.class);
        if (routingContext != null) {
            Token vertxToken = routingContext.get(QuarkusUtils.VERTX_TOKEN_KEY);
            if (vertxToken != null) {
                vertxToken.link();
            }

            // Clears the Vert.x path queue so the vertx-web module's headers-end handler
            // doesn't overwrite the JAX-RS route template name we set below.
            routingContext.data().remove(QuarkusUtils.VERTX_PATH_KEY);
        }

        QuarkusUtils.setTransactionName(requestContext.getTarget());
        Weaver.callOriginal();
    }
}
