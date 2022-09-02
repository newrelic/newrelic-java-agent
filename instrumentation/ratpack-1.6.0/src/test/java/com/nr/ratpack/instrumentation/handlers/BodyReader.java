package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import ratpack.handling.Context;
import ratpack.handling.Handler;

public class BodyReader implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        NewRelic.addCustomParameter("BodyReaderHandler", "yes");

        ctx.getRequest()
                .getBody()
                .then(body -> NewRelic.addCustomParameter("BodyReaderHandlerThen", "yes"));

        ctx.next();
    }
}
