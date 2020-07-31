/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.routing;

import com.agent.instrumentation.spray.InboundWrapper;
import com.agent.instrumentation.spray.PathMatcherUtils;
import com.agent.instrumentation.spray.RequestWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import scala.Function1;
import scala.PartialFunction;

@Weave(type = MatchType.ExactClass, originalName = "spray.routing.HttpServiceBase$class")
public class SprayRoutingHttpServer {

    @Trace(dispatcher = true)
    public static final void runSealedRoute$1(final HttpServiceBase $this, final RequestContext ctx, final PartialFunction sealedExceptionHandler$1, final Function1 sealedRoute$1) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SprayHttp");
        AgentBridge.getAgent().getTransaction().setWebRequest(new RequestWrapper(ctx.request()));
        PathMatcherUtils.reset();
        Weaver.callOriginal();
    }

}
