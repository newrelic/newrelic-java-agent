/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.undertow.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.undertow.Util;
import io.undertow.util.HttpString;
import io.undertow.util.PathTemplateMatcher;

import java.util.Map;
import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "io.undertow.server.RoutingHandler")
public abstract class RoutingHandler_Instrumentation {
    private final Map<HttpString, PathTemplateMatcher<RoutingMatch_Instrumentation>> matches = Weaver.callOriginal();

    public void handleRequest(HttpServerExchange exchange) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Undertow", "HttpHandler", getClass().getSimpleName(), "handleRequest");
        Weaver.callOriginal();

        if (matches != null) {
            PathTemplateMatcher<RoutingMatch_Instrumentation> matcher = matches.get(exchange.getRequestMethod());

            if (matcher != null) {
                PathTemplateMatcher.PathMatchResult<RoutingMatch_Instrumentation> match = matcher.match(exchange.getRelativePath());

                if (match != null) {
                    Util.setWebRequestAndResponse(exchange);
                    Util.addTransactionNamedByParameter(Util.NamedBySource.RoutingHandler);
                    NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "Undertow",
                            Util.createTransactionName(match.getMatchedTemplate(), exchange.getRequestMethod().toString()));
                }
            }
        }
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.undertow.server.RoutingHandler$RoutingMatch")
    private static class RoutingMatch_Instrumentation {
    }
}
