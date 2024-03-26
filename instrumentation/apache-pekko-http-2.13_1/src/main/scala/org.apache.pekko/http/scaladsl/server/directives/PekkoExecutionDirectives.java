/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server.directives;

import org.apache.pekko.http.scaladsl.server.PekkoHttpContextFunction;
import org.apache.pekko.http.scaladsl.server.ExceptionHandler;
import org.apache.pekko.http.scaladsl.server.RejectionHandler;
import org.apache.pekko.http.scaladsl.server.RequestContext;
import org.apache.pekko.http.scaladsl.server.RouteResult;
import org.apache.pekko.http.scaladsl.settings.ParserSettings;
import org.apache.pekko.http.scaladsl.settings.RoutingSettings;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.Future;

@Weave(originalName = "org.apache.pekko.http.scaladsl.server.Route$")
public class PekkoExecutionDirectives {

    public Function1<RequestContext, Future<RouteResult>> seal(Function1<RequestContext, Future<RouteResult>> f1,
            RoutingSettings routingSettings, ParserSettings parserSettings, RejectionHandler rejectionHandler,
            ExceptionHandler exceptionHandler) {
        Function1<RequestContext, Future<RouteResult>> result = Weaver.callOriginal();
        return PekkoHttpContextFunction.contextWrapper(result);
    }

}
