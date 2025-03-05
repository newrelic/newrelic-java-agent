package io.undertow.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.undertow.RequestWrapper;
import com.nr.agent.instrumentation.undertow.Util;

import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "io.undertow.server.Connectors")
public class Connectors_Instrumentation {
    @Trace(dispatcher=true)
    public static void executeRootHandler(HttpHandler handler, HttpServerExchange exchange) {
        Transaction transaction = NewRelic.getAgent().getTransaction();

        String requestPath = exchange.getRequestPath();
        if(requestPath.startsWith("/")) {
            requestPath = requestPath.substring(1);
        }

        Util.addTransactionNamedByParameter(Util.NamedBySource.ConnectorInstrumentation);
        transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "Undertow",
                Util.createTransactionName(requestPath, exchange.getRequestMethod().toString()));

        Weaver.callOriginal();
    }
}
