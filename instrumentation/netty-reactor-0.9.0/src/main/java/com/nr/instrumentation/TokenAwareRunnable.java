package com.nr.instrumentation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

public class TokenAwareRunnable implements Runnable {
    private Runnable delegate;

    private Token token;

    public TokenAwareRunnable(Runnable delegate) {
        this.delegate = delegate;
        this.token = NewRelic.getAgent().getTransaction().getToken();
    }

    @Override
    public void run() {
        if (this.token != null) {
            this.token.linkAndExpire();
            this.token = null;
        }
        delegate.run();
    }
}
