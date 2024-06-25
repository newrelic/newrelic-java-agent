/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl.server;

import akka.http.scaladsl.server.util.Tuple;
import com.agent.instrumentation.akka.http.PathMatcherUtils;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.Future;

@Weave(type = MatchType.BaseClass, originalName = "akka.http.scaladsl.server.Directive$")
public abstract class Directive_Instrumentation<L> {

    public <T> Directive<T> apply(Function1<Function1<T, Function1<RequestContext, Future<RouteResult>>>,
            Function1<RequestContext, Future<RouteResult>>> function1, Tuple<T> tuple) {
        // Wrap any Directives that we see in order to propagate our request context properly
        return new PathMatcherUtils.DirectiveWrapper<>(tuple, (Directive<T>) Weaver.callOriginal());
    }

}
