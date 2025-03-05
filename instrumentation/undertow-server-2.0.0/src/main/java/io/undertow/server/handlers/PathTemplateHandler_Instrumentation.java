package io.undertow.server.handlers;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.undertow.Util;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplateMatcher;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "io.undertow.server.handlers.PathTemplateHandler")
public class PathTemplateHandler_Instrumentation {
    private final PathTemplateMatcher<HttpHandler> pathTemplateMatcher = Weaver.callOriginal();

    public void handleRequest(HttpServerExchange exchange) {
        NewRelic.getAgent().getTracedMethod().setMetricName("Undertow", "HttpHandler", getClass().getSimpleName(), "handleRequest");
        Weaver.callOriginal();

        PathTemplateMatcher.PathMatchResult<HttpHandler> match = pathTemplateMatcher.match(exchange.getRelativePath());
        if (match != null) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- PathTemplateHandler_Instrumentation handleRequest matcher " + exchange.getRequestPath());
            NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- PathTemplateHandler_Instrumentation handleRequest matcher " + match.getMatchedTemplate());

            Util.setWebRequestAndResponse(exchange);
            Util.addTransactionNamedByParameter(Util.NamedBySource.PathTemplateHandler);
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "Undertow",
                    Util.createTransactionName(match.getMatchedTemplate(), exchange.getRequestMethod().toString()));
        }
    }
}
