package com.nr.ratpack.instrumentation;

import com.newrelic.api.agent.NewRelic;
import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;

import java.util.Collections;
import java.util.Map;

public class CustomServerErrorHandler implements ServerErrorHandler {

    @Override
    public void error(Context context, Throwable throwable) throws Exception {
        final Map<String, String> atts = Collections.singletonMap("portland", "style bagel");
        NewRelic.noticeError(throwable, atts);
        context.getResponse().status(500).send(throwable.getMessage());
    }
}
