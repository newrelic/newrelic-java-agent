/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.api.listener;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

@Weave(type = MatchType.ExactClass, originalName = "org.mule.module.http.internal.listener.grizzly.GrizzlyRequestDispatcherFilter")
public abstract class GrizzlyRequestDispatcherFilter_Instrumentation {

    @Trace(dispatcher = true)
    public NextAction handleRead(final FilterChainContext ctx) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Mule", "RequestDispatcher", "handleRequest");
        return Weaver.callOriginal();
    }

}
