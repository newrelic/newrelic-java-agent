package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import ratpack.exec.Blocking;
import ratpack.handling.Context;
import ratpack.handling.Handler;

public class BlockingFolk implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        NewRelic.addCustomParameter("BlockingFolkHandler", "yes");

        Blocking.exec(() -> {
            NewRelic.addCustomParameter("BlockingFolkExec", "yes");
        });

        ctx.next();
    }
}
