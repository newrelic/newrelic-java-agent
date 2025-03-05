package io.undertow.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.undertow.RunnableWrapper;
import io.undertow.util.HeaderMap;

import java.util.concurrent.Executor;
import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "io.undertow.server.HttpServerExchange")
public abstract class HttpServerExchange_Instrumentation {
    @NewField
    public Token token = null;

    public HttpServerExchange_Instrumentation(final ServerConnection connection, final HeaderMap requestHeaders,
            final HeaderMap responseHeaders,  long maxEntitySize) {
        this.token = NewRelic.getAgent().getTransaction().getToken();
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- HttpServerExchange_Instrumentation constructor");
    }

    private void invokeExchangeCompleteListeners() {
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- HttpServerExchange_Instrumentation invokeExchangeCompleteListeners");

        Weaver.callOriginal();

        if (token != null) {
            token.expire();
            token = null;
        }
    }

    public HttpServerExchange_Instrumentation dispatch(Executor executor, Runnable runnable) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- HttpServerExchange_Instrumentation dispatch");

        if (!(runnable instanceof RunnableWrapper)) {
            RunnableWrapper wrapper = new RunnableWrapper(runnable, token);
            runnable = wrapper;
        }

        return Weaver.callOriginal();
    }
}
