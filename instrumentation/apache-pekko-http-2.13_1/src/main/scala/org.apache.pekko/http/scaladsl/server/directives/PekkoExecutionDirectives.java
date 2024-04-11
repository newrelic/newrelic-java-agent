/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server.directives;

import org.apache.pekko.http.scaladsl.model.HttpRequest;
import org.apache.pekko.http.scaladsl.model.HttpResponse;
import org.apache.pekko.http.scaladsl.server.*;
import org.apache.pekko.http.scaladsl.settings.ParserSettings;
import org.apache.pekko.http.scaladsl.settings.RoutingSettings;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.pekko.stream.Materializer;
import scala.Function1;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

@Weave(originalName = "org.apache.pekko.http.scaladsl.server.Route$")
public class PekkoExecutionDirectives {

    private Function1<HttpRequest, Future<HttpResponse>> createAsyncHandler(Function1<RequestContext,
            Future<RouteResult>> sealedRoute, RoutingLog routingLog, RoutingSettings routingSettings,
            ParserSettings parserSettings, ExecutionContextExecutor ec, Materializer mat){
        sealedRoute = PekkoHttpContextFunction.contextWrapper(sealedRoute);
        return Weaver.callOriginal();
    }

}
