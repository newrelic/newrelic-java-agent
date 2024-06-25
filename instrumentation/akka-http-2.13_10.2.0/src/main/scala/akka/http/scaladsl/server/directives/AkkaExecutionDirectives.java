/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl.server.directives;

import akka.http.scaladsl.model.HttpRequest;
import akka.http.scaladsl.model.HttpResponse;
import akka.http.scaladsl.server.AkkaHttpContextFunction;
import akka.http.scaladsl.server.RequestContext;
import akka.http.scaladsl.server.RouteResult;
import akka.http.scaladsl.server.RoutingLog;
import akka.http.scaladsl.settings.ParserSettings;
import akka.http.scaladsl.settings.RoutingSettings;
import akka.stream.Materializer;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

@Weave(originalName = "akka.http.scaladsl.server.Route$")
public class AkkaExecutionDirectives {

    private Function1<HttpRequest, Future<HttpResponse>> createAsyncHandler(
            Function1<RequestContext, Future<RouteResult>> sealedRoute, RoutingLog routingLog, RoutingSettings routingSettings,
            ParserSettings parserSettings, ExecutionContextExecutor ec, Materializer mat){
        sealedRoute = AkkaHttpContextFunction.contextWrapper(sealedRoute);
        return Weaver.callOriginal();
    }

}
