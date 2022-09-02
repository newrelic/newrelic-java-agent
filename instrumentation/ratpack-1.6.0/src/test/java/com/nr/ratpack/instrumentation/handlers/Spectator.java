package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import ratpack.handling.Context;
import ratpack.handling.Handler;

public class Spectator implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH,
                true, "Ratpack", "spectator");

        NewRelic.addCustomParameter("SpectatorHandler", "yes");

        ctx.next();
    }
}
