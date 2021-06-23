/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl.server.directives;

import akka.http.scaladsl.server.AkkaHttpContextFunction;
import akka.http.scaladsl.server.ExceptionHandler;
import akka.http.scaladsl.server.RejectionHandler;
import akka.http.scaladsl.server.RequestContext;
import akka.http.scaladsl.server.RouteResult;
import akka.http.scaladsl.settings.ParserSettings;
import akka.http.scaladsl.settings.RoutingSettings;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.Future;

@Weave(originalName = "akka.http.scaladsl.server.Route$")
public class AkkaExecutionDirectives {

    public Function1<RequestContext, Future<RouteResult>> seal(Function1<RequestContext, Future<RouteResult>> f1,
            RoutingSettings routingSettings, ParserSettings parserSettings, RejectionHandler rejectionHandler,
            ExceptionHandler exceptionHandler) {
        Function1<RequestContext, Future<RouteResult>> result = Weaver.callOriginal();
        return AkkaHttpContextFunction.contextWrapper(result);
    }

}
