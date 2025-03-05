package io.undertow.predicate;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.undertow.Util;
import io.undertow.attribute.ExchangeAttribute;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplate;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "io.undertow.predicate.PathTemplatePredicate")
public abstract class PathTemplatePredicate_Instrumentation {
    private final ExchangeAttribute attribute = Weaver.callOriginal();
    private final PathTemplate value = Weaver.callOriginal();

    public boolean resolve(final HttpServerExchange exchange) {
        boolean result = Weaver.callOriginal();

        if (result) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- Predicate resolve() " + value.getTemplateString());
            NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- Predicate " + exchange.getRequestPath());
            Util.setWebRequestAndResponse(exchange);
            Util.addTransactionNamedByParameter(Util.NamedBySource.PathTemplatePredicate);
            NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "Undertow",
                    Util.createTransactionName(value.getTemplateString(), exchange.getRequestMethod().toString()));
        }

        return result;
    }
}
