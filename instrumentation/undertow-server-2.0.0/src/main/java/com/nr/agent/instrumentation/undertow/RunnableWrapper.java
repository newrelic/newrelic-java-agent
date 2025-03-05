package com.nr.agent.instrumentation.undertow;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import java.util.logging.Level;

public class RunnableWrapper implements Runnable {
    private final Runnable delegate;
    private Token token;

    public RunnableWrapper(Runnable delegate, Token token) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- RunnableWrapper constructor");

        this.delegate = delegate;
        this.token = token;
    }

    public void run() {
        NewRelic.getAgent().getLogger().log(Level.INFO, "DUF-- RunnableWrapper run()");

        if (token != null) {
            token.link();
            token = null;
        }
        if (delegate != null) {
            delegate.run();
        }
    }
}
