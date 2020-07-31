/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.sun.jersey.server.impl.model.method.dispatch;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.sun.jersey.api.core.HttpContext;

@Weave(type = MatchType.BaseClass, originalName = "com.sun.jersey.server.impl.model.method.dispatch.ResourceJavaMethodDispatcher")
public abstract class ResourceJavaMethodDispatcher_Instrumentation {

    @Trace
    public final void dispatch(Object resource, HttpContext context) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("Jersey", getClass().getName(), "dispatch");
        Weaver.callOriginal();
    }

}
