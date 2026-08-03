/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.jboss.resteasy.reactive.server.handlers;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.QuarkusUtils;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

@Weave(originalName = "org.jboss.resteasy.reactive.server.handlers.InvocationHandler")
public class InvocationHandler_Instrumentation {

    @Trace
    public void handle(ResteasyReactiveRequestContext requestContext) {
        QuarkusUtils.setTransactionName(requestContext.getTarget());
        Weaver.callOriginal();
    }
}
