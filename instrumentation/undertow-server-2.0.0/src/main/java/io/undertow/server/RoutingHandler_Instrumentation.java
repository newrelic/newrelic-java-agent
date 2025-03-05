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
                NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- RoutingHandler_Instrumentation handleRequest matcher " + exchange.getRequestPath());

                PathTemplateMatcher.PathMatchResult<RoutingMatch_Instrumentation> match = matcher.match(exchange.getRelativePath());

                if (match != null) {
                    Util.setWebRequestAndResponse(exchange);
                    NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- RoutingHandler_Instrumentation handleRequest match " + match.getMatchedTemplate());

                    Util.addTransactionNamedByParameter(Util.NamedBySource.RoutingHandler);
                    NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "Undertow",
                            Util.createTransactionName(match.getMatchedTemplate(), exchange.getRequestMethod().toString()));
                }
            }
        }

        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- RoutingHandler_Instrumentation handleRequest exchange response " + exchange.getResponseContentLength());
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- RoutingHandler_Instrumentation handleRequest exchange response " + exchange.getStatusCode());
    }

    @Weave(type = MatchType.ExactClass, originalName = "io.undertow.server.RoutingHandler$RoutingMatch")
    private static class RoutingMatch_Instrumentation {
    }
}
