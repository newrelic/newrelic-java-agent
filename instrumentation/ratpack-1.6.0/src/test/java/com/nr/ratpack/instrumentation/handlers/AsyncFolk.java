package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.handling.Handler;

public class AsyncFolk implements Handler {
    @Override
    public void handle(Context ctx) throws Exception {
        NewRelic.addCustomParameter("AsyncFolkHandler", "yes");

        Promise.async(downstream -> {
            NewRelic.addCustomParameter("AsyncFolkHandlerUpstream", "yes");
            downstream.success("yes");
        }).then(result -> {
            NewRelic.addCustomParameter("AsyncFolkHandlerThen", result.toString());
            ctx.render(result.toString());
        });
    }
}
